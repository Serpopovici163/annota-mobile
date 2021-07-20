package com.example.annotamobile;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.example.annotamobile.ui.library.LibraryFragment;
import com.example.annotamobile.ui.settings.SettingsFragment;

import static com.example.annotamobile.MainActivity.getCurrentScreen;
import static com.example.annotamobile.MainActivity.getNavView;

public class GestureListener extends GestureDetector.SimpleOnGestureListener {

    private static int MIN_SWIPE_DIST_X = 300;
    private static int MIN_SWIPE_DIST_Y = 300;

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
        if (getActivity().findViewById(R.id.pictureView) != null)
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
                Fragment libraryFragment = new LibraryFragment(); //Might be buggy but this took me two hours so fuck it, gestureListener doesn't have access to things like getView() so changing a fragment was a pain
                getNavView().setVisibility(View.INVISIBLE);
                getActivity().getSupportActionBar().setTitle(R.string.title_library);
                getActivity().getSupportFragmentManager().beginTransaction().replace(getActivity().getSupportFragmentManager().getFragments().get(0).getId(), libraryFragment).addToBackStack("libraryFragment").commit();
            } else {
                //down
                Fragment settingsFragment = new SettingsFragment();
                getNavView().setVisibility(View.INVISIBLE);
                getActivity().getSupportActionBar().setTitle(R.string.title_settings);
                getActivity().getSupportFragmentManager().beginTransaction().replace(getActivity().getSupportFragmentManager().getFragments().get(0).getId(), settingsFragment).addToBackStack("settingsFragment").commit();
            }
        }

        return true;
    }
}
