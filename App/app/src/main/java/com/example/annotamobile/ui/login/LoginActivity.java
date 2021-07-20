package com.example.annotamobile.ui.login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.annotamobile.MainActivity;
import com.example.annotamobile.R;
import com.example.annotamobile.databinding.ActivityLoginBinding;
import com.example.annotamobile.ui.NetworkIO;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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
        NetworkIO networkIO = new NetworkIO();

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
                if (loginViewModel.loginDataChanged(usernameEditText.getText().toString(), passwordEditText.getText().toString()))
                    loginButton.setEnabled(true);
                else
                    loginButton.setEnabled(false);
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                NetworkIO networkIO = new NetworkIO();
                networkIO.login(getApplicationContext(), usernameEditText.getText().toString(), passwordEditText.getText().toString(), new NetworkIO.NetworkIOListener() {
                    @Override
                    public void onSuccess(@Nullable String[] data) {
                        loadingProgressBar.setVisibility(View.INVISIBLE);
                        updateUiWithUser(getApplicationContext(), networkIO.getName(getApplicationContext()));
                    }
                });
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
                        networkIO.register(context, email, password, name, new NetworkIO.NetworkIOListener() {
                            @Override
                            public void onSuccess(@Nullable String[] data) {
                                updateUiWithUser(context, networkIO.getName(context));
                            }
                        });
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.passwords_not_equal, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        //now that all the listeners have been initialized, we can finally check if the user has a valid auth key and therefore bypass the login screen
        networkIO.keycheck(getApplicationContext(), new NetworkIO.NetworkIOListener() {
            @Override
            public void onSuccess(@Nullable String[] data) {
                updateUiWithUser(context, networkIO.getName(context));
            }
        });


        //if nothing happens then just make sure the text fields are clear
        binding.username.setText("");
        binding.password.setText("");
        binding.confirmPassword.setText("");
        binding.name.setText("");
    }

    public void updateUiWithUser(Context context, String user_name) {
        String welcome = context.getResources().getString(R.string.login_welcome) + " " + user_name + "!";
        Toast.makeText(context, welcome, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }


}