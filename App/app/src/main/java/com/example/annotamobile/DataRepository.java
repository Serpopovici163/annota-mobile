package com.example.annotamobile;

public class DataRepository {
    //this class only contains variables relevant to the code, all text is stored in res/values/strings.xml
    public static String server_url = "http://ludicroustech.ca:8080";
    public static String auth_key_filename = "auth.key";
    public static String temp_pic_filename = "temp.jpg";

    //server codes, writing these as vars since they may change in the future
    public static String auth_key_ok = "AUTH_KEY_VALID";
    public static String auth_key_bad = "AUTH_KEY_DENIED";
    public static String bad_request = "INVALID_REQUEST_TYPE";
    public static String bad_login = "INVALID_LOGIN";
    public static String logout_ok = "LOGOUT_DONE";
    public static String registration_error = "REGISTRATION_ERROR";

    //random
    public static float penSize = 20; //pen color is in colors.xml
}
