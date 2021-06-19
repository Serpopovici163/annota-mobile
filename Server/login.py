#handles all login, logout, or keycheck type requests
import essentials
import uuid
import re

def checkUserPassword(conn, email, password):
    #first check that email exists before querying for passwords
    sql = "SELECT email FROM user_data"
    cur = conn.cursor()
    cur.execute(sql)
    real_emails = cur.fetchall()
    emailExists = False
    for emails in real_emails:
        if (email == emails[0]):
            emailExists = True
    
    #if email is invalid, return INVALID_LOGIN
    if (not emailExists):
        essentials.message(2,"REQUEST DENIED : Invalid email")
        return "INVALID_LOGIN"

    sql = "SELECT password FROM user_data WHERE email=\"" + email +"\""
    cur = conn.cursor()
    cur.execute(sql)
    real_password = cur.fetchall()[0][0]

    if (password == real_password):
        #generate UID and assign it to sql database before returning it to user
        user_uuid = str(uuid.uuid1())
        essentials.message(1,"Login success! Assigning UUID [" + user_uuid + "]")

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
        essentials.message(2,"REQUEST DENIED : Invalid password")
        return "INVALID_LOGIN"

def checkUID(conn, uuid, name):
    #when sending names, android adds a + instead of a space so this line replaces that with a space
    name = name.replace("+"," ")
    sql = "SELECT uids FROM user_data WHERE name=\"" + name + "\""
    cur = conn.cursor()
    cur.execute(sql)
    real_uuids = cur.fetchall()[0][0].split("/")

    for real_uuid in real_uuids:
        if uuid == real_uuid:
            return True
    
    return False

def logout(conn, uuid, name):
    #first we obtain uuids associated with given name and then we can proceed to delete the uuid if it exists
    sql = "SELECT uids FROM user_data WHERE name=" + name.replace("+", " ")
    cur = conn.cursor()
    cur.execute(sql)
    active_uuids = cur.fetchall()[0][0].split("/")
    updated_uuid_list = ""

    #cycle through uuid list and drop uuid provided in request
    for uuids in active_uuids:
        if (uuid != uuids):
            #check if updated_uuid_list is empty
            if (updated_uuid_list == ""):
                updated_uuid_list = uuids
            else:
                #add / delimiter and append uuid
                updated_uuid_list += "/"
                updated_uuid_list += uuids
    
    #write uuids back to database
    sql = "UPDATE user_data SET uids=\"" + updated_uuid_list + "\" WHERE name=\"" + name.replace("+", " ") + "\""
    cur = conn.cursor()
    cur.execute(sql)
    conn.commit()

def register(conn, email, password, name) :
    name = name.replace("+", " ") #just in case android is on its bullshit again
    regex = '\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b'
    #validate email
    print("[" + email + "]")
    if ("@" not in email) :
        #invalid so return error
        return "INVALID_EMAIL"
    #basically all I'm gonna check, we can proceed to registering account
    uid = str(uuid.uuid1())
    sql = "INSERT INTO user_data(uids,email,password,name) VALUES(\"" + uid + "\",\"" + email + "\",\"" + password + "\",\"" + name + "\")"
    cur = conn.cursor()
    cur.execute(sql)
    conn.commit()

    return uid + ";" + name