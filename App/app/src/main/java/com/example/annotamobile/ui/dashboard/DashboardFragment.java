package com.example.annotamobile.ui.dashboard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.annotamobile.AnnotationFragment;
import com.example.annotamobile.FileIO;
import com.example.annotamobile.R;
import com.example.annotamobile.databinding.FragmentDashboardBinding;
import com.example.annotamobile.ui.login.LoginActivity;
import com.google.common.util.concurrent.ListenableFuture;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.mukesh.DrawingView;
import com.ortiz.touchview.TouchImageView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import cz.msebera.android.httpclient.Header;

import static com.example.annotamobile.DataRepository.*;

public class DashboardFragment extends Fragment implements View.OnClickListener {

    private FragmentDashboardBinding binding;

    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 1;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private File file;
    private ImageCapture.OutputFileOptions outputOptions;

    private PreviewView previewView = null;
    private DrawingView drawingView = null;
    private Button take_photo;
    private ImageButton crop_button;
    private ImageButton cancel_button;
    private ImageButton send_button;
    private TouchImageView pictureView = null;
    private CameraSelector cameraSelector;
    private RelativeLayout progressBar;

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                getActivity(),
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(
                getActivity(),
                CAMERA_PERMISSION,
                CAMERA_REQUEST_CODE
        );
    }

    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(
                getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(
                getActivity(),
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                CAMERA_REQUEST_CODE
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //initialize the camera, first we need to check for permission
        if (hasCameraPermission()) {
            //locate and attach the previewView screen to the initialized camera
            previewView = view.findViewById(R.id.previewView);

            cameraProviderFuture = ProcessCameraProvider.getInstance(getActivity().getApplicationContext());
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    Preview preview = new Preview.Builder().build();
                    imageCapture = new ImageCapture.Builder().setTargetRotation(Surface.ROTATION_0).build();
                    cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
                    preview.setSurfaceProvider(previewView.createSurfaceProvider());
                    cameraProvider.bindToLifecycle(this, cameraSelector,
                            preview, imageCapture);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }, ContextCompat.getMainExecutor(getActivity().getApplicationContext()));
        } else {
            requestCameraPermission();
        }

        //check storage permission
        if (!hasStoragePermission()) {
            requestStoragePermission();
        }

        file = new File(getContext().getFilesDir(), temp_pic_filename);
        outputOptions = new ImageCapture.OutputFileOptions.Builder(file).build();

        //delete any pre-existing picture file
        if (file.exists())
            file.delete();//*/

        //initialize pictureView and buttons once previewView is live
        pictureView = binding.getRoot().findViewById(R.id.pictureView);
        drawingView = binding.getRoot().findViewById(R.id.drawingView);
        progressBar = binding.getRoot().findViewById(R.id.loadingPanel);

        take_photo = binding.getRoot().findViewById(R.id.take_photo);
        take_photo.setOnClickListener(this);
        crop_button = binding.getRoot().findViewById(R.id.crop_button);
        crop_button.setOnClickListener(this);
        cancel_button = binding.getRoot().findViewById(R.id.cancel_button);
        cancel_button.setOnClickListener(this);
        send_button = binding.getRoot().findViewById(R.id.send_button);
        send_button.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_photo:
                //regenerate file and store picture
                try {
                    file.createNewFile();

                    imageCapture.takePicture(outputOptions, Executors.newSingleThreadExecutor(), new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull @NotNull ImageCapture.OutputFileResults outputFileResults) {
                            //now that the image has been saved we can begin the cropping process
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //first let's swap the visible elements to change the user's screen
                                    take_photo.setVisibility(View.INVISIBLE);
                                    previewView.setVisibility(View.INVISIBLE);
                                    crop_button.setVisibility(View.VISIBLE);
                                    cancel_button.setVisibility(View.VISIBLE);
                                    send_button.setVisibility(View.VISIBLE);

                                    //load image into pictureView
                                    pictureView.setImageResource(0);
                                    pictureView.setImageURI(Uri.fromFile(file));
                                    pictureView.setVisibility(View.VISIBLE);
                                    pictureView.setEnabled(true);
                                    pictureView.resetZoom();
                                    drawingView.clear();
                                    crop_button.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.button_enabled));
                                    //set up drawingView but keep it invisible for now since it blocks the touchimageview otherwise
                                    drawingView.setPenSize(penSize);
                                }
                            });
                        }

                        @Override
                        public void onError(@NonNull @NotNull ImageCaptureException exception) {
                            exception.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                } break;

            case R.id.cancel_button:

                //first lets check if the drawing view is empty
                drawingView.setDrawingCacheEnabled(true);
                Bitmap drawing = Bitmap.createBitmap(drawingView.getDrawingCache());
                drawingView.setDrawingCacheEnabled(false);
                Bitmap emptyBitmap = Bitmap.createBitmap(drawing.getWidth(), drawing.getHeight(), drawing.getConfig());

                if (!drawing.sameAs(emptyBitmap)) { //if there is some drawing, the cancel button will first clear the drawing before deleting the picture
                    drawingView.clear();
                    break;
                }

                //just swap back to the old view
                take_photo.setVisibility(View.VISIBLE);
                previewView.setVisibility(View.VISIBLE);
                crop_button.setVisibility(View.INVISIBLE);
                cancel_button.setVisibility(View.INVISIBLE);
                send_button.setVisibility(View.INVISIBLE);
                pictureView.setVisibility(View.INVISIBLE);
                drawingView.setVisibility(View.INVISIBLE);

                break;

            case R.id.crop_button:
                //crop button freezes pinch to zoom and allows user to draw
                if (pictureView.isEnabled()) { //fix the image in place and enable drawing, pen colour must be declared here to work
                    pictureView.setEnabled(false);
                    drawingView.setVisibility(View.VISIBLE);
                    drawingView.setPenColor(ContextCompat.getColor(getContext(), R.color.pen_color));
                    drawingView.initializePen();
                    crop_button.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.button_disabled));
                } else { //allow the picture to be moved and disable/clear drawing
                    pictureView.setEnabled(true);
                    drawingView.clear();
                    drawingView.setVisibility(View.INVISIBLE);
                    crop_button.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.button_enabled));
                } break;

            case R.id.send_button:
                //ship image to server, first let's obtain the cropped version of the image itself alongside the user's drawing
                pictureView.setDrawingCacheEnabled(true);
                drawingView.setDrawingCacheEnabled(true);

                Bitmap croppedImage = Bitmap.createBitmap(pictureView.getDrawingCache());
                Bitmap userDrawing = Bitmap.createBitmap(drawingView.getDrawingCache());

                pictureView.setDrawingCacheEnabled(false);
                drawingView.setDrawingCacheEnabled(false);

                //check if the userDrawing image is empty in which case we won't send it to the server
                emptyBitmap = Bitmap.createBitmap(userDrawing.getWidth(), userDrawing.getHeight(), userDrawing.getConfig());

                if (userDrawing.sameAs(emptyBitmap))
                    userDrawing = null;

                //we can now send both of these to the server
                //we also need to obtain the local uuid to authenticate
                FileIO fileIO = new FileIO();
                String[] file_data = fileIO.readFromFile(auth_key_filename, getContext()).split(";");
                AsyncHttpClient client = new AsyncHttpClient();
                RequestParams params = new RequestParams();
                params.put("data", "TRANSCRIBE;" + file_data[0] + ";" + file_data[1] + ";" + imageToString(croppedImage) + ";" + imageToString(userDrawing));
                progressBar.setVisibility(View.VISIBLE);
                client.post(server_url, params, new AsyncHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        //check response
                        progressBar.setVisibility(View.INVISIBLE);
                        String response_string = new String(responseBody, StandardCharsets.UTF_8);
                        if (Objects.equals(response_string, auth_key_bad)) {
                            //auth key is no longer valid --> logout user
                            LoginActivity instance = new LoginActivity();
                            instance.logout();
                        } else {
                            //auth key good, we can start parsing response
                            if (Objects.equals(response_string.split(";")[0], transcribe_empty)) {
                                //notify the user that Google did not find any text
                                Toast.makeText(getContext(), R.string.empty_annotation, Toast.LENGTH_LONG).show();
                            }
                            //now lets open the annotation_details fragment to complete this request
                            finalizeAnnotation(response_string.split(";")[1]);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        progressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(getContext(), R.string.server_error, Toast.LENGTH_LONG).show();
                        error.printStackTrace();
                    }
                });

                //just swap back to the old view
                take_photo.setVisibility(View.VISIBLE);
                previewView.setVisibility(View.VISIBLE);
                crop_button.setVisibility(View.INVISIBLE);
                cancel_button.setVisibility(View.INVISIBLE);
                send_button.setVisibility(View.INVISIBLE);
                pictureView.setVisibility(View.INVISIBLE);
                drawingView.setVisibility(View.INVISIBLE);

                break;
        }
        //delete picture file
        file.delete();
    }

    private void finalizeAnnotation(String index) {
        Bundle data = new Bundle();
        data.putString("index", index);
        AnnotationFragment annotFrag = new AnnotationFragment();
        annotFrag.setArguments(data);
        getActivity().getSupportFragmentManager().beginTransaction().replace(((ViewGroup)getView().getParent()).getId(), annotFrag).addToBackStack("annotFrag").commit();
    }

    private String imageToString(Bitmap bitmap) { //converts a bitmap to a string so it can be sent to the server

        //check if image is null in which case we will return an empty string
        if (bitmap == null)
            return "";

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        byte[] imageBytes = output.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}