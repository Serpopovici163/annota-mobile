package com.example.annotamobile;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.annotamobile.databinding.ActivityMainBinding;
import com.example.annotamobile.ui.NetworkIO;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.jetbrains.annotations.Nullable;

import static androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;

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
        NetworkIO networkIO = new NetworkIO();
        networkIO.keycheck(getApplicationContext(), new NetworkIO.NetworkIOListener() {
            @Override
            public void onSuccess(@Nullable String[] data) {

            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetectorCompat.onTouchEvent(event);
        return true;
    }

    @Override
    public void onBackPressed() {

        String tag;

        try {
            //get top fragment
            int index = getSupportFragmentManager().getBackStackEntryCount() - 1;
            FragmentManager.BackStackEntry backEntry = getSupportFragmentManager().getBackStackEntryAt(index);
            tag = backEntry.getName();
        } catch (Exception e) {
            return;
        }

        //make sure the nav bar is enabled again and remove the topmost fragment
        getNavView().setVisibility(View.VISIBLE);
        getSupportFragmentManager().popBackStack(tag, POP_BACK_STACK_INCLUSIVE);

    }
}