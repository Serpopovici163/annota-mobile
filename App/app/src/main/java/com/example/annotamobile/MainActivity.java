package com.example.annotamobile;

import android.os.Bundle;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.annotamobile.databinding.ActivityMainBinding;
import com.example.annotamobile.ui.login.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;

import static com.example.annotamobile.DataRepository.auth_key_filename;
import static com.example.annotamobile.DataRepository.auth_key_ok;
import static com.example.annotamobile.DataRepository.server_url;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static BottomNavigationView navView = null;
    private GestureDetectorCompat gestureDetectorCompat = null;

    public static BottomNavigationView getNavView() {
        return MainActivity.navView;
    }

    public static int getCurrentScreen() {
        return navView.getSelectedItemId();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        //initialize swipe detection
        GestureListener gestureListener = new GestureListener();
        gestureListener.setActivity(this);
        gestureDetectorCompat = new GestureDetectorCompat(this, gestureListener);

        //set camera as starting screen
        navView.setSelectedItemId(R.id.navigation_dashboard);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //every time app resumes, it will double check its auth key to ensure it is logged in
        try {
            FileIO fileIO = new FileIO();
            String[] file_data = fileIO.readFromFile(auth_key_filename, getApplicationContext()).split(";");
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
                        if (Objects.equals(response_string, auth_key_ok)) {
                            //user is still logged in so we do nothing
                            return;
                        } else {
                            LoginActivity instance = new LoginActivity();
                            instance.logout();
                        }
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetectorCompat.onTouchEvent(event);
        return true;
    }
}