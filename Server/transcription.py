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

    #first add the overlay to the image
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
    cv2.imwrite("data\\" + email + "\\" + date_time + "\\crop.png", opencvImage)

    '''
    os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = r'googleAPI_key.json'
    client = vision.ImageAnnotatorClient()

    FILE_PATH = os.path.join(os.getcwd(), 'data', email, date_time, 'combination.jpg')

    with io.open(FILE_PATH, 'rb') as image_file:
        content = image_file.read()

    image = vision.Image(content=content)
    response = client.document_text_detection(image=image)

    print(response.full_text_annotation.text) '''

    return "DONE"

    