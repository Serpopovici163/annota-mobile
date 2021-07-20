package com.example.annotamobile.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.annotamobile.BuildConfig;
import com.example.annotamobile.R;
import com.example.annotamobile.ui.NetworkIO;

import org.jetbrains.annotations.NotNull;

import static android.content.Context.MODE_PRIVATE;
import static com.example.annotamobile.DataRepository.pref_filename;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        preferences = requireActivity().getSharedPreferences(pref_filename, MODE_PRIVATE);
        addPreferencesFromResource(R.xml.fragment_settings);
        findPreference("version_info").setSummary(BuildConfig.VERSION_NAME);

        findPreference("logout").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                NetworkIO networkIO = new NetworkIO();
                networkIO.logout(requireActivity().getApplicationContext(), R.string.logout);
                return false;
            }
        });
    }

    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        assert view != null;
        view.setBackgroundColor(requireContext().getColor(R.color.black)); //the top should fix this maybe?
        return view;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        Log.e("Pref","Call");

        return false;
    }
}