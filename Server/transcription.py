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
    
    sql = "CREATE TABLE directory(id INTEGER PRIMARY KEY AUTOINCREMENT, dateTime DATETIME, name TEXT, content TEXT, comments TEXT, cat1 TEXT, cat2 TEXT, cat3 TEXT);"

    cur = conn.cursor()
    cur.execute(sql)
    conn.close()

def getCatList(email):
    #converting cat_list from two dimensional array to string with delimiters
    #sql request returns a two dimensional array where the child arrays are cat1, cat2, and cat3 for each entry
    
    userDataConn = essentials.create_connection(os.path.join(os.getcwd(), 'data', email, 'directory.db'))
    cat1_list = ""
    cat2_list = ""
    cat3_list = ""

    #using SELECT DISTINCT helps eliminate some duplicates but now we must eliminate the rest
    sql = "SELECT DISTINCT cat1 FROM directory"
    cur = userDataConn.cursor()
    cur.execute(sql)
    data = cur.fetchall()

    for cat1 in data:
        if cat1_list == "":
            cat1_list = essentials.removeNoneType(cat1[0])
        else:
            cat1_list = cat1_list + "," + essentials.removeNoneType(cat1[0])

    sql = "SELECT DISTINCT cat2 FROM directory"
    cur = userDataConn.cursor()
    cur.execute(sql)
    data = cur.fetchall()

    for cat2 in data:
        if cat2_list == "":
            cat2_list = essentials.removeNoneType(cat2[0])
        else:
            cat2_list = cat2_list + "," + essentials.removeNoneType(cat2[0])

    sql = "SELECT DISTINCT cat3 FROM directory"
    cur = userDataConn.cursor()
    cur.execute(sql)
    data = cur.fetchall()

    for cat3 in data:
        if cat3_list == "":
            cat3_list = essentials.removeNoneType(cat3[0])
        else:
            cat3_list = cat3_list + "," + essentials.removeNoneType(cat3[0])

    return essentials.removeNoneType(cat1_list) + ";" + essentials.removeNoneType(cat2_list) + ";" + essentials.removeNoneType(cat3_list)

def transcribeImage(conn, auth_key, croppedImage, userDrawing):
    #all user data will be saved in a folder with their email
    email = login.getEmail(conn, auth_key)

    #check if a folder exists for the user, otherwise we must initialize the user
    if not os.path.exists(os.path.join(os.getcwd(), 'data', email)):
        initializeUser(email)

    #now lets get the current system time to save the images
    date_time = datetime.now().strftime("%Y-%m-%d-%H-%M-%S")
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
        #the next three lines open the combination.png file with opencv, convert the colourspace to HSV (hue, saturation, value) and use this colourspace to mask out the user's drawing
        opencvImage = cv2.imread("data\\" + email + "\\" + date_time + "\\image.jpg")    
        opencvContour = cv2.imread("data\\" + email + "\\" + date_time + "\\drawing.jpg")
        hsv = cv2.cvtColor(opencvContour, cv2.COLOR_BGR2HSV)
        mask = cv2.inRange(hsv, np.array([0, 0, 180]), np.array([180,255,255])) 

        #now that we have an isolated outline of the user's drawing we can document where exactly this is
        contours, _ = cv2.findContours(mask, cv2.RETR_LIST, cv2.CHAIN_APPROX_NONE)

        #these three lines take the newfound contours and generate a black and white mask to use for isolating the circled text
        mask[:] = 0
        for con in contours:
            mask = cv2.drawContours(mask, [con], -1, 255, -1)  

        #now the text is isolated and saved in crop.png which can be sent to Google for transcription
        opencvImage[mask != 255] = (255,255,255)

        #finally add back the purple line to opencvImage so we isolate exactly what the user circled
        mask = cv2.inRange(hsv, np.array([0, 0, 180]), np.array([180,255,255]))
        opencvImage[mask == 255] = (255,255,255)

    else:
        #if the overlay is empty then simply open the image, this part also deletes the empty drawing file
        opencvImage = cv2.imread("data\\" + email + "\\" + date_time + "\\image.jpg") 
        os.remove(os.path.join(os.getcwd(), "data", email, date_time, "drawing.jpg"))
        
    cv2.imwrite("data\\" + email + "\\" + date_time + "\\crop.png", opencvImage)

    #we can now combine image and drawing to create a cropped lower res version (icon) for the app's listview
    image = Image.open(os.path.join(os.getcwd(), "data", email, date_time, "image.jpg"))
    #drawing = Image.open(os.path.join(os.getcwd(), "data", email, date_time, "drawing.jpg"))
    #image.paste(drawing, (0,0))
    width, height = image.size

    #from here we can make a square assuming that height > width
    top = (height - width) / 2
    bottom = top + width

    image = image.crop((0, top, width, bottom))
    image = image.resize((80,80), Image.ANTIALIAS)
    image.save(os.path.join(os.getcwd(), "data", email, date_time, "icon.png"))

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
    cat_list = getCatList(email)

    essentials.message(1, "TRANSCRIBE_OK;" + str(index) + ";" + cat_list)
    if content == "":
        return "TRANSCRIBE_EMPTY;" + str(index) + ";" + cat_list
    else:
        return "TRANSCRIBE_OK;" + str(index) + ";" + cat_list

def completeTranscription(conn, auth_key, id, name, comments, cat1, cat2, cat3):
    #we need to get the email from the user_data table
    email = login.getEmail(conn, auth_key)

    userDataConn = essentials.create_connection(os.path.join(os.getcwd(), 'data', email, 'directory.db'))
    sql = "UPDATE directory SET name=\"" + name.replace("+"," ") + "\", comments=\"" + comments.replace("+"," ") + "\", cat1=\"" + cat1.replace("+"," ") + "\", cat2=\"" + cat2.replace("+"," ") + "\", cat3=\"" + cat3.replace("+"," ") + "\" WHERE id=" + id + ";"
    cur = userDataConn.cursor()
    cur.execute(sql)
    userDataConn.commit()

    essentials.message(1, "Data saved successfully")

    return "REQUEST_OK"

def search(conn, auth_key, criteria_type, data1, data2, data3):
    #user email is necessary to access their data
    email = login.getEmail(conn, auth_key)
    userDataConn = essentials.create_connection(os.path.join(os.getcwd(), 'data', email, 'directory.db'))
    sql = ""

    #catch if no criteria_data included in request
    if ((data1 == "") and (data2 == "") and (data3 == "")):
        essentials.message(2, "No search data provided, returning whole library")
        sql = "SELECT id, name, dateTime, content, comments, cat1, cat2, cat3 FROM directory"
    else:
        if criteria_type == "0": #content
            sql = "SELECT id, name, dateTime, content, comments, cat1, cat2, cat3 FROM directory WHERE (name LIKE '%" + data1 + "%') OR (content LIKE '%" + data1 + "%') OR (comments LIKE '%" + data1 + "%')"
        elif criteria_type == "1": #category
            sql = "SELECT id, name, dateTime, content, comments, cat1, cat2, cat3 FROM directory WHERE"

            if not data1 == "":
                sql = sql + " (cat1=\"" + data1 + "\")"
                
            if not data2 == "":
                if sql[-1] == ")":
                    # there was a cat1 criteria so we will also need to append the AND symbol
                    sql = sql + " AND"
                sql = sql + " (cat2=\"" + data2 + "\")"
                
            if not data3 == "":
                if sql[-1] == ")":
                    # there was a cat1 or cat2 criteria so we will also need to append the AND symbol
                    sql = sql + " AND"
                sql = sql + " (cat3=\"" + data3 + "\")"
        elif criteria_type == "2": #date
            #index 0 is minimum and index 1 is max
            if data1 == "":
                sql = "SELECT id, name, dateTime, content, comments, cat1, cat2, cat3 FROM directory WHERE dateTime < '" + data2 + "'"
            elif data2 == "":
                sql = "SELECT id, name, dateTime, content, comments, cat1, cat2, cat3 FROM directory WHERE dateTime > '" + data1 + "'"
            else:
                sql = "SELECT id, name, dateTime, content, comments, cat1, cat2, cat3 FROM directory WHERE (dateTime < '" + data2 + "') AND (dateTime > '" + data1 + "'"
        else:
            return "INVALID_REQUEST"

    cur = userDataConn.cursor()
    cur.execute(sql)
    data = cur.fetchall()
    response = ""

    for entry in data:

        #images are stored in folders named by the time and date of their creation
        with open(os.path.join(os.getcwd(), 'data', email, str(entry[2]), "icon.png"), "rb") as icon:
            iconString = str(base64.b64encode(icon.read()))

        if response == "":
            response = essentials.sqlToString(entry) + "&" + iconString
        else:
            response = response + ";" + essentials.sqlToString(entry) + "&" + iconString

    return "REQUEST_OK;" + response

def handleCatRequest(conn, auth_key):
    email = login.getEmail(conn, auth_key)
    cat_list = getCatList(email)
    
    return "REQUEST_OK;" + cat_list

def getThumbnail(conn, auth_key, dateTime):
    email = login.getEmail(conn, auth_key)

    with open(os.path.join(os.getcwd(), 'data', email, dateTime, "image.jpg"), "rb") as thumbnail:
        return "REQUEST_OK;" + str(base64.b64encode(thumbnail.read()))

def updateItem(conn, auth_key, id, name, content, comments, cat1, cat2, cat3):
    email = login.getEmail(conn, auth_key)

    userDataConn = essentials.create_connection(os.path.join(os.getcwd(), 'data', email, 'directory.db'))
    sql = "UPDATE directory SET name=\"" + name.replace("+"," ") + "\", content=\"" + content.replace("+"," ") + "\", comments=\"" + comments.replace("+"," ") + "\", cat1=\"" + cat1.replace("+"," ") + "\", cat2=\"" + cat2.replace("+"," ") + "\", cat3=\"" + cat3.replace("+"," ") + "\" WHERE id=" + id + ";"
    cur = userDataConn.cursor()
    cur.execute(sql)
    userDataConn.commit()

    essentials.message(1, "Data saved successfully")

    return "REQUEST_OK"