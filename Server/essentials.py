import sqlite3
from sqlite3 import Error

#all global static variables and some basic functions

hostName = "192.168.23.195"
serverPort = 8080
database = r"annota.db"

def create_connection(db_file):
    conn = None
    try:
        conn = sqlite3.connect(db_file)
    except Error as e:
        print(e)

    return conn

def message(priority, message):
    if (priority == 1):
        #Debug
        print("[DEBUG] : " + message)
    elif (priority == 2):
        #Warning
        print("[WARNING] : " + message)
    elif (priority == 3):
        #Warning
        print("[ERROR] : " + message)

def removeHex(data):
    data = data.decode("utf-8")
    while data.find("%") != -1:
        index = data.find("%")
        string = data[index + 1]
        string += data[index + 2]
        searchString = data[index]
        searchString += data[index + 1]
        searchString += data[index + 2]
        
        if (searchString == "%0A"): #0A represents new line but we do not want a new line char in our data
            data = data.replace(searchString, "")
        else:
            data = data.replace(searchString, bytes.fromhex(string).decode("utf-8"))
    return data