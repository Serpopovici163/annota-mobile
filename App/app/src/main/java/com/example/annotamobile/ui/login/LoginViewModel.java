package com.example.annotamobile.ui.login;

import android.content.Context;
import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.annotamobile.DataRepository;
import com.example.annotamobile.FileIO;
import com.example.annotamobile.R;
import com.example.annotamobile.data.LoginRepository;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;

import static com.example.annotamobile.DataRepository.auth_key_filename;
import static com.example.annotamobile.DataRepository.bad_login;
import static com.example.annotamobile.DataRepository.bad_request;

public class LoginViewModel extends ViewModel {

    private MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private LoginRepository loginRepository;

    LoginViewModel(LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
    }

    LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }

    LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public void login(String username, String password, Context context) {

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
                            //login user
                            String s = new String(response, StandardCharsets.UTF_8);
                            loginResult.setValue(new LoginResult(new LoggedInUserView(s.split(";")[1])));
                        } else {
                            loginResult.setValue(new LoginResult(R.string.login_failed));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                    // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                    //check for timeout
                    loginResult.setValue(new LoginResult(R.string.no_internet));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loginDataChanged(String username, String password) {
        if (!isUserNameValid(username)) {
            loginFormState.setValue(new LoginFormState(R.string.invalid_username, null));
        } else if (!isPasswordValid(password)) {
            loginFormState.setValue(new LoginFormState(null, R.string.invalid_password));
        } else {
            loginFormState.setValue(new LoginFormState(true));
        }
    }

    // A placeholder username validation check
    private boolean isUserNameValid(String username) {
        if (username == null) {
            return false;
        }
        if (username.contains("@")) {
            return Patterns.EMAIL_ADDRESS.matcher(username).matches();
        } else {
            return !username.trim().isEmpty();
        }
    }

    // A placeholder password validation check
    private boolean isPasswordValid(String password) {
        return password != null && password.trim().length() > 4;
    }
}