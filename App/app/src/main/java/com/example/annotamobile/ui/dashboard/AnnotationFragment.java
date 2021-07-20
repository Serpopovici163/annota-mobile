package com.example.annotamobile.ui.dashboard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.annotamobile.MainActivity;
import com.example.annotamobile.R;
import com.example.annotamobile.ui.NetworkIO;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.jetbrains.annotations.Nullable;

public class AnnotationFragment extends Fragment implements View.OnClickListener {

    Bundle data = null;

    private BottomNavigationView navigationView;
    private EditText annotationName;
    private EditText annotationComments;
    private AutoCompleteTextView cat1;
    private AutoCompleteTextView cat2;
    private AutoCompleteTextView cat3;
    private Button submit;

    @Override
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //hide nav bar to make this fullscreen
        navigationView = getActivity().findViewById(R.id.nav_view);
        navigationView.setVisibility(View.INVISIBLE);

        getActivity().setContentView(R.layout.fragment_annotation);
        data =  this.getArguments();

        //set all the bindings in this fragment
        annotationName = getActivity().findViewById(R.id.annotationName);
        annotationComments = getActivity().findViewById(R.id.annotationComments);
        cat1 = getActivity().findViewById(R.id.annotationCat1);
        cat2 = getActivity().findViewById(R.id.annotationCat2);
        cat3 = getActivity().findViewById(R.id.annotationCat3);
        submit = getActivity().findViewById(R.id.submitAnnotationDataButton);
        submit.setOnClickListener(this);

        //set up the autocomplete for categories
        ArrayAdapter<String> cat1_adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, data.getString("cat1").split(","));
        ArrayAdapter<String> cat2_adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, data.getString("cat2").split(","));
        ArrayAdapter<String> cat3_adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, data.getString("cat3").split(","));

        cat1.setAdapter(cat1_adapter);
        cat2.setAdapter(cat2_adapter);
        cat3.setAdapter(cat3_adapter);

        cat1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cat1.showDropDown();
                return false;
            }
        });

        cat2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cat2.showDropDown();
                return false;
            }
        });

        cat3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cat3.showDropDown();
                return false;
            }
        });

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_annotation, container, false);
    }

    @Override
    public void onClick(View v) {
        //first check if all fields are completed as needed
        if (annotationName.getText().toString().matches("") || annotationComments.getText().toString().matches("") || (cat1.getText().toString().matches("") && cat2.getText().toString().matches("") && cat3.getText().toString().matches(""))) {
            Toast.makeText(getContext(), R.string.incompleteAnnotationData, Toast.LENGTH_LONG).show();
        } else {
            //good to go
            NetworkIO networkIO = new NetworkIO();
            networkIO.transcribe_info(requireContext(), data.getString("index"), annotationName.getText().toString(), annotationComments.getText().toString(), cat1.getText().toString(), cat2.getText().toString(), cat3.getText().toString(), new NetworkIO.NetworkIOListener() {
                @Override
                public void onSuccess(@Nullable String[] data) {
                    endFragment();
                }
            });
        }
    }

    private void endFragment() {
        Intent dismissFragment = new Intent(getContext(), MainActivity.class);
        dismissFragment.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(dismissFragment);
    }

    @Override
    public void onDestroy() {
        navigationView.setVisibility(View.VISIBLE);
        super.onDestroy();
    }
}