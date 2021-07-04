package com.example.annotamobile;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.annotamobile.ui.login.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;

import static com.example.annotamobile.DataRepository.*;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_annotation, container, false);
    }

    @Override
    public void onClick(View v) {
        //first check if all fields are completed as needed
        if (annotationName.getText().toString().matches("") || annotationComments.getText().toString().matches("") || cat1.getText().toString().matches("") || cat2.getText().toString().matches("") || cat3.getText().toString().matches("")) {
            Toast.makeText(getContext(), R.string.incompleteAnnotationData, Toast.LENGTH_LONG).show();
        } else {
            //good to go
            FileIO fileIO = new FileIO();
            String[] file_data = fileIO.readFromFile(auth_key_filename, getContext()).split(";");
            String requestData = "TRANSCRIBE_INFO;" + file_data[0] + ";" + file_data[1] + ";" + data.getString("index") + ";" + annotationName.getText().toString() + ";" + annotationComments.getText().toString() + ";" + cat1.getText().toString() + ";" + cat2.getText().toString() + ";" + cat3.getText().toString();

            //send request to server
            AsyncHttpClient client = new AsyncHttpClient();
            RequestParams params = new RequestParams();
            params.put("data", requestData);
            client.post(server_url, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    //check response
                    String response_string = new String(responseBody, StandardCharsets.UTF_8);
                    if (!Objects.equals(response_string, request_ok)) {
                        //error occurred so we will let the user know what happened
                        Toast.makeText(getContext(), R.string.server_error, Toast.LENGTH_LONG).show();
                    }
                    //regardless, we need to deflate this fragment and return to the app
                    endFragment();
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable error) {
                    // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                    Toast.makeText(getContext(), R.string.server_error, Toast.LENGTH_LONG).show();
                    error.printStackTrace();
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