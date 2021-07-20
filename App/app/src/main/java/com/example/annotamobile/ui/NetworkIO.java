package com.example.annotamobile.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.annotamobile.DataRepository;
import com.example.annotamobile.FileIO;
import com.example.annotamobile.R;
import com.example.annotamobile.ui.login.LoginActivity;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;

import static com.example.annotamobile.DataRepository.auth_key_bad;
import static com.example.annotamobile.DataRepository.auth_key_filename;
import static com.example.annotamobile.DataRepository.auth_key_ok;
import static com.example.annotamobile.DataRepository.bad_login;
import static com.example.annotamobile.DataRepository.bad_request;
import static com.example.annotamobile.DataRepository.registration_error;
import static com.example.annotamobile.DataRepository.request_ok;
import static com.example.annotamobile.DataRepository.server_url;
import static com.example.annotamobile.DataRepository.transcribe_empty;

public class NetworkIO {

    //listener initialization
    public interface NetworkIOListener {
        public void onSuccess(@Nullable String[] data);
    }

    public String getUUID(Context context) {
        try {
            FileIO fileIO = new FileIO();
            return fileIO.readFromFile(auth_key_filename, context).split(";")[0];
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public String getName(Context context) {
        try {
            FileIO fileIO = new FileIO();
            return fileIO.readFromFile(auth_key_filename, context).split(";")[1];
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public void getCatList(Context context, NetworkIOListener listener) {

        String[] cat_list = {"","",""};

        try {
            AsyncHttpClient client = new AsyncHttpClient();
            // Http Request Params Object
            RequestParams params = new RequestParams();
            params.put("data", "GET_CAT_LIST;" + getUUID(context) + ";" + getName(context));
            client.post(DataRepository.server_url, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    try { //first check if request worked and if so return data
                        String response_string = new String(response, StandardCharsets.UTF_8);
                        String[] response_split = response_string.split(";");
                        if (Objects.equals(response_split[0], request_ok)) {

                            cat_list[0] = response_split[1];
                            cat_list[1] = response_split[2];
                            cat_list[2] = response_split[3];

                            if (listener != null)
                                listener.onSuccess(cat_list);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                    // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                    //do nothing
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void login(Context context, String username, String password, NetworkIOListener listener) {
        try {
            AsyncHttpClient client = new AsyncHttpClient();
            // Http Request Params Object
            RequestParams params = new RequestParams();
            params.put("data", "LOGIN;" + username + ";" + password);
            client.post(DataRepository.server_url, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    try {
                        //save uid and name for future use
                        String response_string = new String(response, StandardCharsets.UTF_8);

                        //check if login was successful
                        if (!Objects.equals(response_string, bad_login) || !Objects.equals(response_string, bad_request)) {
                            FileIO fileIO = new FileIO();
                            fileIO.writeToFile(response_string, auth_key_filename, context);
                            //send callback
                            if (listener != null)
                                listener.onSuccess(null);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                    // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                    //do nothing
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void register(Context context, String email, String password, String name, NetworkIOListener listener) {
        try {
            AsyncHttpClient client = new AsyncHttpClient();
            RequestParams params = new RequestParams();
            params.put("data", "REGISTER;" + email + ";" + password + ";" + name);
            client.post(server_url, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    //check response
                    String response_string = new String(response, StandardCharsets.UTF_8);
                    if (!Objects.equals(response_string, registration_error) || !Objects.equals(response_string, bad_request)) {
                        //user registered successfully
                        FileIO fileIO = new FileIO();
                        fileIO.writeToFile(response_string, auth_key_filename, context);
                        if (listener != null)
                            listener.onSuccess(null);
                    } else {
                        logout(context, R.string.logout);
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                    // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                    //Genuinely don't understand why a try/catch is needed here but Java's a bitch
                    try {
                        throw e;
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logout(Context context, int messageRef) { //delete auth key file and notify user
        FileIO fileIO = new FileIO();
        Toast.makeText(context, messageRef, Toast.LENGTH_LONG).show();

        //check if file exists and if so, attempt to deauth local key
        String[] file_data = fileIO.readFromFile(auth_key_filename, context).split(";");
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("data", "LOGOUT;" + file_data[0] + ";" + file_data[1]);
        client.post(server_url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                fileIO.deleteFile(auth_key_filename, context);
                //redirect to login screen
                Intent loginActivity = new Intent(context, LoginActivity.class);
                loginActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                loginActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(loginActivity);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
                return;
            }
        });
    }

    public void keycheck(Context context, NetworkIOListener listener) {
        try {
            FileIO fileIO = new FileIO();
            String[] file_data = fileIO.readFromFile(auth_key_filename, context).split(";");

            if (file_data[0] != "") {
                //send request to server
                AsyncHttpClient client = new AsyncHttpClient();
                RequestParams params = new RequestParams();
                params.put("data", "KEYCHECK;" + file_data[0] + ";" + file_data[1]);
                client.post(server_url, params, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        //check response
                        String response_string = new String(responseBody, StandardCharsets.UTF_8);
                        if (Objects.equals(response_string, auth_key_ok))
                            listener.onSuccess(null);
                        else
                            logout(context, R.string.logout);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable error) {
                        // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                        error.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void transcribe(Context context, View progressBar, String croppedImage, String userDrawing, NetworkIOListener listener) {

        String result[] = {"","","",""};

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("data", "TRANSCRIBE;" + getUUID(context) + ";" + getName(context) + ";" + croppedImage + ";" + userDrawing);
        progressBar.setVisibility(View.VISIBLE);
        client.post(server_url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                //check response
                progressBar.setVisibility(View.INVISIBLE);
                String response_string = new String(responseBody, StandardCharsets.UTF_8);
                if (Objects.equals(response_string, auth_key_bad)) {
                    //auth key is no longer valid --> logout user
                    logout(context, R.string.logout);
                } else {
                    //auth key good, we can start parsing response
                    if (Objects.equals(response_string.split(";")[0], transcribe_empty)) {
                        //notify the user if Google did not find any text
                        Toast.makeText(context, R.string.empty_annotation, Toast.LENGTH_LONG).show();
                    }
                    //now lets open the annotation_details fragment to complete this request
                    result[0] = response_string.split(";")[1];
                    try {
                        result[1] = response_string.split(";")[2];
                        result[2] = response_string.split(";")[3];
                        result[3] = response_string.split(";")[4];
                    } catch (Exception e) {
                        Log.w("TRANSCRIBE","No categories");
                    }

                    listener.onSuccess(result);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(context, R.string.server_error, Toast.LENGTH_LONG).show();
                error.printStackTrace();
            }
        });
    }

    public void transcribe_info(Context context, String index, String name, String comments, String cat1, String cat2, String cat3, NetworkIOListener listener) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        String requestData = "TRANSCRIBE_INFO;" + getUUID(context) + ";" + getName(context) + ";" + index + ";" + name + ";" + comments + ";" + cat1 + ";" + cat2 + ";" + cat3;
        params.put("data", requestData);
        client.post(server_url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                //check response
                String response_string = new String(responseBody, StandardCharsets.UTF_8);
                if (!Objects.equals(response_string, request_ok)) {
                    //error occurred so we will let the user know what happened
                    Toast.makeText(context, R.string.server_error, Toast.LENGTH_LONG).show();
                }
                listener.onSuccess(null);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable error) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                Toast.makeText(context, R.string.server_error, Toast.LENGTH_LONG).show();
                error.printStackTrace();
            }
        });
    }

    public void search(Context context, int position, String[] data, NetworkIOListener listener) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        String requestData = "SEARCH;" + getUUID(context) + ";" + getName(context) + ";" + String.valueOf(position) + ";" + data[0] + ";" + data[1] + ";" + data[2];
        params.put("data", requestData);
        client.post(server_url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String[] response = new String(responseBody, StandardCharsets.UTF_8).split(";");
                if (Objects.equals(response[0], bad_request))
                    Toast.makeText(context, R.string.server_error, Toast.LENGTH_LONG).show();
                else
                    listener.onSuccess(response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(context, R.string.server_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void getItemPicture(Context context, String dateTime, NetworkIOListener listener) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        String requestData = "GET_THUMBNAIL;" + getUUID(context) + ";" + getName(context) + ";" + dateTime;
        params.put("data", requestData);
        client.post(server_url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String[] response = new String(responseBody, StandardCharsets.UTF_8).split(";");
                if (Objects.equals(response[0], bad_request))
                    Toast.makeText(context, R.string.server_error, Toast.LENGTH_LONG).show();
                else
                    listener.onSuccess(response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(context, R.string.server_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void updateItem(Context context, String id, String name, String content, String comments, String cat1, String cat2, String cat3, NetworkIOListener listener) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        String requestData = "UPDATE_ITEM;" + getUUID(context) + ";" + getName(context) + ";" + id + ";" + name + ";" + content + ";" + comments + ";" + cat1 + ";" + cat2 + ";" + cat3;
        params.put("data", requestData);
        client.post(server_url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = new String(responseBody, StandardCharsets.UTF_8);
                if (Objects.equals(response, bad_request))
                    Toast.makeText(context, R.string.server_error, Toast.LENGTH_LONG).show();
                else
                    listener.onSuccess(null);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(context, R.string.server_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    public Bitmap stringToBitmap(String imageString) {
        try{
            byte [] encodeByte = Base64.decode(imageString.split("'")[1], Base64.DEFAULT);//Base64.decode(imageString,Base64.NO_PADDING);
            return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
        }
        catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
}