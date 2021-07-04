import base64
import login
import essentials
import sqlite3
import os, io
import cv2
import numpy as np
from datetime import datetime
from google.cloud import vision
from PIL import Image

def initializeUser(email):
    #first make a folder for the user
    os.makedirs(os.path.join(os.getcwd(), 'data', email))
    #second initialize a database to keep track of their annotations
    conn = sqlite3.connect(os.path.join(os.getcwd(), 'data', email, 'directory.db'))
    
    sql = "CREATE TABLE directory(id INTEGER PRIMARY KEY AUTOINCREMENT, dateTime TEXT, name TEXT, content TEXT, comments TEXT, cat1 TEXT, cat2 TEXT, cat3 TEXT);"

    cur = conn.cursor()
    cur.execute(sql)
    conn.close()

def transcribeImage(conn, auth_key, name, croppedImage, userDrawing):
    
    #first lets check if user auth key is valid
    if not login.checkUID(conn, auth_key, name):
        essentials.message(1, "Invalid AUTH KEY")
        essentials.message(3, "REQUEST DENIED")
        return "AUTH_KEY_DENIED"
    else:
        essentials.message(1, "REQUEST VALIDATED")

    #all user data will be saved in a folder with their email
    email = login.getEmail(conn, auth_key)

    #check if a folder exists for the user, otherwise we must initialize the user
    if not os.path.exists(os.path.join(os.getcwd(), 'data', email)):
        initializeUser(email)

    #now lets get the current system time to save the images
    date_time = datetime.now().strftime("%d-%m-%Y-%H-%M-%S")
    os.mkdir(os.path.join(os.getcwd(), 'data', email, date_time))

    #finally we can write save the images
    f = open("data\\" + email + "\\" + date_time + "\\image.jpg", "wb")
    f.write(base64.b64decode(croppedImage))
    f.close()

    f = open("data\\" + email + "\\" + date_time + "\\drawing.jpg", "wb")
    f.write(base64.b64decode(userDrawing))
    f.close()

    essentials.message(1, "Images saved successfuly, processing now")

    #check if overlay image is empty (in case the user didn't draw anything)
    if os.stat("data\\" + email + "\\" + date_time + "\\drawing.jpg").st_size != 0:
        #if overlay exists, open both to combine them and process the image afterwards
        background = Image.open("data\\" + email + "\\" + date_time + "\\image.jpg").convert("RGBA")
        overlay = Image.open("data\\" + email + "\\" + date_time + "\\drawing.jpg").convert("RGBA")

        combination = Image.blend(background, overlay, 0.5)
        combination.save("data\\" + email + "\\" + date_time + "\\combination.png", "PNG")

        #the next three lines open the combination.png file with opencv, convert the colourspace to HSV (hue, saturation, value) and use this colourspace to mask out the user's drawing
        opencvImage = cv2.imread("data\\" + email + "\\" + date_time + "\\combination.png")    
        hsv = cv2.cvtColor(opencvImage, cv2.COLOR_BGR2HSV)
        mask = cv2.inRange(hsv, np.array([0, 0, 180]), np.array([180,255,255])) 

        #now that we have an isolated outline of the user's drawing we can document where exactly this is
        contours, _ = cv2.findContours(mask, cv2.RETR_LIST, cv2.CHAIN_APPROX_NONE)

        #these three lines take the newfound contours and generate a black and white mask to use for isolating the circled text
        mask[:] = 0
        for con in contours:
            mask = cv2.drawContours(mask, [con], -1, 255, -1)   

        #now the text is isolated and saved in crop.png which can be sent to Google for transcription
        opencvImage[mask != 255] = (255,255,255)
    else:
        # if the overlay is empty then simply open the image, this part also deletes the empty drawing file
        opencvImage = cv2.imread("data\\" + email + "\\" + date_time + "\\image.jpg") 
        os.remove(os.path.join(os.getcwd(), "data", email, date_time, "drawing.jpg"))
        
    cv2.imwrite("data\\" + email + "\\" + date_time + "\\crop.png", opencvImage)

    '''
    os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = r'googleAPI_key.json'
    client = vision.ImageAnnotatorClient()

    FILE_PATH = os.path.join(os.getcwd(), 'data', email, date_time, 'crop.png')

    with io.open(FILE_PATH, 'rb') as image_file:
        content = image_file.read()

    image = vision.Image(content=content)
    response = client.document_text_detection(image=image)

    content = response.full_text_annotation.text '''

    content = "Google API call disabled"

    #add all the data we have so far to the database and return the index of our entry
    userDataConn = essentials.create_connection(os.path.join(os.getcwd(), 'data', email, 'directory.db'))
    sql = "INSERT INTO directory (dateTime, content) VALUES ('" + date_time + "','" + content + "');"
    cur = userDataConn.cursor()
    cur.execute(sql)
    userDataConn.commit()

    #now let's yoink the index and ship that back to the server
    sql = "SELECT id FROM directory WHERE dateTime='" + date_time + "';"
    cur = userDataConn.cursor()
    cur.execute(sql)
    index = cur.fetchall()[0][0]

    #finally, we need to obtain all the pre-existing categories in our directory and send those as well
    #cat1,cat2,cat3
    sql = "SELECT DISTINCT cat1,cat2,cat3 FROM directory;"
    cur = userDataConn.cursor()
    cur.execute(sql)
    category_data = cur.fetchall()
    category_string = ""

    #converting cat_list from two dimensional array to string with delimiters
    for category_list in category_data:
        for category in category_list:
            if not category is None:
                if category_string == "":
                    category_string = category
                elif category_string[-1] == ';':
                    category_string = category_string + category
                else:
                    category_string = category_string + "," + category
        category_string = category_string + ";"

    #the code above will add an extra ';' at the end so we can remove that here
    category_string = category_string[:-1]

    if content == "":
        return "TRANSCRIBE_EMPTY;" + str(index) + category_string  
    else:
        return "TRANSCRIBE_OK;" + str(index) + category_string   

def completeTranscription(conn, auth_key, userName, index, name, comments, cat1, cat2, cat3):
    #authenticate user before proceeding
    if not login.checkUID(conn, auth_key, userName):
        return "AUTH_KEY_DENIED"

    #we need to get the email from the user_data table
    userName = userName.replace("+", " ") #just in case android is on its bullshit again
    sql = "SELECT email FROM user_data WHERE name=\"" + userName + "\";"
    cur = conn.cursor()
    cur.execute(sql)
    email = cur.fetchall()[0][0]

    
    userDataConn = essentials.create_connection(os.path.join(os.getcwd(), 'data', email, 'directory.db'))
    sql = "UPDATE directory SET name=\"" + name + "\", comments=\"" + comments + "\", cat1=\"" + cat1 + "\", cat2=\"" + cat2 + "\", cat3=\"" + cat3 + "\" WHERE id=" + index + ";"
    cur = userDataConn.cursor()
    cur.execute(sql)
    userDataConn.commit()

    essentials.message(1, "Data saved successfully")

    return "REQUEST_OK"