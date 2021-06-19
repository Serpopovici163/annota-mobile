import sqlite3
from sqlite3 import Error
from http.server import BaseHTTPRequestHandler, HTTPServer

#our files
import essentials
import login

conn = None

def create_connection(db_file):
    conn = None
    try:
        conn = sqlite3.connect(db_file)
    except Error as e:
        print(e)

    return conn

def handleRequest(data):
    data_split = data.split(';')
    if (data_split[0] == 'LOGIN'):
        #login request, email will be index 1 and password index 2
        essentials.message(1,"Received LOGIN request")
        return login.checkUserPassword(conn, data_split[1], data_split[2])
    elif (data_split[0] == 'KEYCHECK'):
        #client has pre-existing auth key
        essentials.message(1, "Received AUTH KEY check request")
        data_split[1] = data_split[1].replace("%0A", "")

        #get each uid from the table and check if given uid exists
        if (login.checkUID(conn, data_split[1], data_split[2])):
            #valid uuid
            essentials.message(1, "REQUEST VALIDATED")
            return "AUTH_KEY_VALID"
        else:
            essentials.message(1, "Invalid AUTH KEY")
            essentials.message(3, "REQUEST DENIED")
            return "AUTH_KEY_DENIED"
    elif (data_split[0] == 'LOGOUT'):
        essentials.message(1, "Received LOGOUT deauthing key [" + data_split[1] + "]")
        login.logout(conn,data_split[1],data_split[2])
        return "LOGOUT_DONE"
    elif (data_split[0] == 'REGISTER'):
        essentials.message(1, "Registering email [" + data_split[1] + "]")
        return login.register(conn,data_split[1],data_split[2],data_split[3]) #args are: conn, email, password, name
    else:
        essentials.message(3, "Invalid request type")
        essentials.message(1, data)
        return "INVALID_REQUEST_TYPE"

class MyServer (BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response_only(404)

    def do_POST(self):
        print("\nGot POST")
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        #got data, now we can differentiate request, first we need to reformat the string from what was sent. Any special characters will be messed up (passwords may be an issue)
        #TODO change all special characters from %xx to whatever they should be, I think xx is hex for ASCII codes but I'm not smart 
        #FIXED using removeHex function in essentials
        post_data = essentials.removeHex(post_data)
        post_data = post_data.replace("data=","")

        response = handleRequest(post_data)
        
        #send response
        self.send_response(200)
        self.send_header('Content-type', 'text/data')
        self.end_headers()
        self.wfile.write(bytes(response,'utf-8'))    

conn = create_connection(essentials.database)
webServer = HTTPServer((essentials.hostName, essentials.serverPort), MyServer)
print("Server started http://%s:%s" % (essentials.hostName, essentials.serverPort))

try:
    webServer.serve_forever()
except KeyboardInterrupt:
    pass

webServer.server_close()
print("Server stopped")