package com.example.annotamobile.ui.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.annotamobile.DataRepository;
import com.example.annotamobile.FileIO;
import com.example.annotamobile.MainActivity;
import com.example.annotamobile.R;
import com.example.annotamobile.databinding.ActivityLoginBinding;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;

import static com.example.annotamobile.DataRepository.auth_key_filename;
import static com.example.annotamobile.DataRepository.auth_key_ok;
import static com.example.annotamobile.DataRepository.bad_request;
import static com.example.annotamobile.DataRepository.registration_error;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    private ActivityLoginBinding binding;
    private static Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LoginActivity.context = getApplicationContext();

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        final EditText confirmPasswordEditText = binding.confirmPassword;
        final EditText nameEditText = binding.name;
        final Button loginButton = binding.login;
        final Button registerButton = binding.register;
        final ProgressBar loadingProgressBar = binding.loading;

        loginViewModel.getLoginFormState().observe(this, new Observer<LoginFormState>() {
            @Override
            public void onChanged(@Nullable LoginFormState loginFormState) {
                if (loginFormState == null) {
                    return;
                }
                loginButton.setEnabled(loginFormState.isDataValid());
                if (loginFormState.getUsernameError() != null) {
                    usernameEditText.setError(getString(loginFormState.getUsernameError()));
                }
                if (loginFormState.getPasswordError() != null) {
                    passwordEditText.setError(getString(loginFormState.getPasswordError()));
                }
            }
        });

        loginViewModel.getLoginResult().observe(this, new Observer<LoginResult>() {
            @Override
            public void onChanged(@Nullable LoginResult loginResult) {
                if (loginResult == null) {
                    return;
                }
                loadingProgressBar.setVisibility(View.GONE);
                if (loginResult.getError() != null) {
                    showLoginFailed(loginResult.getError());
                }
                if (loginResult.getSuccess() != null) {
                    updateUiWithUser(loginResult.getSuccess());

                    //Complete and destroy login activity once successful
                    setResult(Activity.RESULT_OK);
                    finish();
                }
            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginViewModel.login(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString(), getApplicationContext());
                }
                return false;
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                loginViewModel.login(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString(), getApplicationContext());
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //first lets check if confirm password and name fields are visible
                if (confirmPasswordEditText.getVisibility() == View.INVISIBLE || nameEditText.getVisibility() == View.INVISIBLE) {
                    //set fields visible so the user can fill them out
                    confirmPasswordEditText.setVisibility(View.VISIBLE);
                    nameEditText.setVisibility(View.VISIBLE);
                } else {
                    //fields are visible
                    String email = usernameEditText.getText().toString();
                    String password = passwordEditText.getText().toString();
                    String confirm_password = confirmPasswordEditText.getText().toString();
                    String name = nameEditText.getText().toString();

                    if (email.isEmpty() || password.isEmpty() || confirm_password.isEmpty() || name.isEmpty()) {
                        //Ask user to fill up all fields
                        Toast.makeText(getApplicationContext(), R.string.incomplete_form, Toast.LENGTH_LONG).show();
                        //we'll ignore whether or not the email is actually an email or whether the name is an actual name since the first one is checked by the server and the second is irrelevant
                        //we'll jump to checking if password strings are the same before submitting a register request
                    } else if (Objects.equals(password, confirm_password)) {
                        //strings are equal so we can send a request
                        try {
                            AsyncHttpClient client = new AsyncHttpClient();
                            RequestParams params = new RequestParams();
                            params.put("data", "REGISTER;" + email + ";" + password + ";" + name);
                            client.post(DataRepository.server_url, params, new AsyncHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                                    //check response
                                    String response_string = new String(response, StandardCharsets.UTF_8);
                                    if (!Objects.equals(response_string, registration_error) || !Objects.equals(response_string, bad_request)) {
                                        //user registered successfully
                                        FileIO fileIO = new FileIO();
                                        fileIO.writeToFile(response_string, auth_key_filename, context);
                                        updateUiWithUser(new LoggedInUserView(response_string.split(";")[1]));
                                    } else {
                                        //delete auth key file and notify user
                                        Toast.makeText(getApplicationContext(), R.string.registration_failed, Toast.LENGTH_LONG).show();
                                        //redirect to login screen
                                        Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
                                        startActivity(loginActivity);
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
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.passwords_not_equal, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        //now that all the listeners have been initialized, we'll check if auth.key file exists and submit AUTH KEY CHECK request if True
        try {
            FileIO fileIO = new FileIO();
            String[] file_data = fileIO.readFromFile(auth_key_filename, getApplicationContext()).split(";");
            if (file_data[0] != "") {
                //send request to server
                AsyncHttpClient client = new AsyncHttpClient();
                RequestParams params = new RequestParams();
                params.put("data", "KEYCHECK;" + file_data[0] + ";" + file_data[1]);
                client.post(DataRepository.server_url, params, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                        //check response
                        String response_string = new String(response, StandardCharsets.UTF_8);
                        if (Objects.equals(response_string, auth_key_ok)) {
                            //log in user
                            updateUiWithUser(new LoggedInUserView(file_data[1]));
                        } else {
                            //delete auth key file and notify user
                            Toast.makeText(getApplicationContext(), R.string.auto_login_failed, Toast.LENGTH_LONG).show();
                            fileIO.deleteFile(auth_key_filename, getApplicationContext());
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateUiWithUser(LoggedInUserView view) {
        String welcome = context.getResources().getString(R.string.login_ok_welcome) + " " + view.getDisplayName() + "!";
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void logout() { //delete auth key file and notify user
        FileIO fileIO = new FileIO();
        Toast.makeText(getApplicationContext(), R.string.logout, Toast.LENGTH_LONG).show();
        fileIO.deleteFile(auth_key_filename, getApplicationContext());
        //redirect to login screen
        Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(loginActivity);
    }

    public void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }

    public static Context getLoginContext() {
        return LoginActivity.context;
    }
}