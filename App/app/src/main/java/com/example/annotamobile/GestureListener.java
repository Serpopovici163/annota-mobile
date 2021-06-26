package com.example.annotamobile;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.example.annotamobile.ui.dashboard.DashboardFragment;

import static com.example.annotamobile.MainActivity.getCurrentScreen;
import static com.example.annotamobile.MainActivity.getNavView;

public class GestureListener extends GestureDetector.SimpleOnGestureListener {

    private static int MIN_SWIPE_DIST_X = 100;
    private static int MIN_SWIPE_DIST_Y = 100;

    private static int MAX_SWIPE_DIST_X = 1000;
    private static int MAX_SWIPE_DIST_Y = 1000;

    private MainActivity activity = null;

    public MainActivity getActivity() {
        return activity;
    }

    public void setActivity(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        //before we do anything let's check if the user is cropping an image in which case we will ignore any swipes as they may be drawing
        if (getActivity().findViewById(R.id.pictureView).getVisibility() == View.VISIBLE)
            return false;

        float deltaX = e1.getX() - e2.getX();
        float deltaY = e1.getY() - e2.getY();

        float deltaXAbs = Math.abs(deltaX);
        float deltaYAbs = Math.abs(deltaY);

        //left and right
        if (deltaXAbs >= MIN_SWIPE_DIST_X && deltaXAbs <= MAX_SWIPE_DIST_X) {
            if (deltaX < 0) {
                //left swipe
                if (getCurrentScreen() == R.id.navigation_dashboard) {
                    //on camera screen
                    getNavView().setSelectedItemId(R.id.navigation_home);
                } else if (getCurrentScreen() == R.id.navigation_notifications) {
                    //on to-do screen
                    getNavView().setSelectedItemId(R.id.navigation_dashboard);
                }
            } else {
                //right swipe
                if (getCurrentScreen() == R.id.navigation_home) {
                    //on annota screen
                    getNavView().setSelectedItemId(R.id.navigation_dashboard);
                } else if (getCurrentScreen() == R.id.navigation_dashboard) {
                    //on camera screen
                    getNavView().setSelectedItemId(R.id.navigation_notifications);
                }
            }
        }

        //up and down
        if (deltaYAbs >= MIN_SWIPE_DIST_Y && deltaYAbs <= MAX_SWIPE_DIST_Y) {
            if (deltaY > 0) {
                //up
            } else {
                //down
            }
        }

        return true;
    }
}
