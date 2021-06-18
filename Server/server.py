import sqlite3
from sqlite3 import Error
from http.server import BaseHTTPRequestHandler, HTTPServer
import uuid

hostName = "192.168.23.195"
serverPort = 8080
database = r"annota.db"
conn = None

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

def create_connection(db_file):
    conn = None
    try:
        conn = sqlite3.connect(db_file)
    except Error as e:
        print(e)

    return conn

def checkUserPassword(email, password):
    sql = "SELECT password FROM user_data WHERE email=\"" + email +"\""
    cur = conn.cursor()
    cur.execute(sql)
    real_password = cur.fetchall()[0][0]

    message(1,"Got email [" + email + "] and password [" + password + "] whereas real password is [" + real_password + "]")

    if (password == real_password):
        #generate UID and assign it to sql database before returning it to user
        user_uuid = str(uuid.uuid1())
        message(1,"Login success! Assigning UUID [" + user_uuid + "]")

        #get UIDs from database in case there are pre-existing ones
        sql = "SELECT uids FROM user_data WHERE email=\"" + email +"\""
        cur = conn.cursor()
        cur.execute(sql)
        uids = cur.fetchall()[0][0]

        if (uids == ""):
            uids = user_uuid
        else:
            uids = uids + "/" + user_uuid

        #TODO add uid to database since this code doesn't do that
        sql = "UPDATE user_data SET uids=\"" + uids + "\"WHERE email=\"" + email + "\""
        cur = conn.cursor()
        cur.execute(sql)
        conn.commit()

        sql = "SELECT name FROM user_data WHERE email=\"" + email +"\""
        cur = conn.cursor()
        cur.execute(sql)
        uids = cur.fetchall()[0][0]
        return user_uuid + ";" + uids #uids is name here, didn't make a new variable
    else:
        message(2,"REQUEST DENIED")
        return "INVALID_LOGIN"

def checkUID(uuid):
    sql = "SELECT uids FROM user_data"
    cur = conn.cursor()
    cur.execute(sql)
    real_uuids = cur.fetchall()

    message(1,"Got uuid [" + uuid + "]")

    for uuid_sublist in real_uuids:
        for uuids in uuid_sublist:
            if "/" in uuids:
                #multiple uuids in database
                real_uuid_list = uuids.split("/")
                for real_uuid in real_uuid_list:
                    message(1,"Real uuid [" + real_uuid + "]")
                    if uuid == real_uuid:
                        return True
            else:
                #single uuid
                message(1,"Real uuid [" + uuids + "]")
                if uuid == uuids:
                    return True
    return False

def handleRequest(data):
    data_split = data.split(';')
    if (data_split[0] == 'LOGIN'):
        #login request, email will be index 1 and password index 2
        message(1,"Received LOGIN request")
        return checkUserPassword(data_split[1], data_split[2])
    elif (data_split[0] == 'KEYCHECK'):
        #client has pre-existing auth key
        message(1, "Received AUTH KEY check request")
        data_split[1] = data_split[1].replace("%0A", "")

        #get each uid from the table and check if given uid exists
        if (checkUID(data_split[1])):
            #valid uuid
            message(1, "REQUEST VALIDATED")
            return "AUTH_KEY_VALID"
        else:
            message(1, "Invalid AUTH KEY")
            message(3, "REQUEST DENIED")
            return "AUTH_KEY_DENIED"
    else:
        message(3, "Invalid request type")
        message(1, data)
        return "INVALID_REQUEST_TYPE"

class MyServer (BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response_only(404)

    def do_POST(self):
        print("Got POST")
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        #got data, now we can differentiate request, first we need to reformat the string from what was sent. Any special characters will be messed up (passwords may be an issue)
        #TODO change all special characters from %xx to whatever they should be, I think xx is hex for ASCII codes but I'm not smart <-- confirmed but adding "utf-8" to decode() fixed it
        #FIXED
        post_data = post_data.decode("utf-8")
        post_data = post_data.replace("%3B",";")
        post_data = post_data.replace("%40","@")
        post_data = post_data.replace("data=","")

        response = handleRequest(post_data)
        
        #send response
        self.send_response(200)
        self.send_header('Content-type', 'text/data')
        self.end_headers()
        self.wfile.write(bytes(response,'utf-8'))    

conn = create_connection(database)
webServer = HTTPServer((hostName, serverPort), MyServer)
print("Server started http://%s:%s" % (hostName, serverPort))

try:
    webServer.serve_forever()
except KeyboardInterrupt:
    pass

webServer.server_close()
print("Server stopped")