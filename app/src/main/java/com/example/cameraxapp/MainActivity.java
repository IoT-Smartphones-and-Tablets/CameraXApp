package com.example.cameraxapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cameraxapp.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            result -> {
                if (result) {
                    initCameraProvider();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonOpenCamera.setOnClickListener(view -> {
            int pGranted = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA);
            if (pGranted == PackageManager.PERMISSION_GRANTED) {
                Log.d("infoAA", "permission granted");
                //we open the camera
                initCameraProvider();
            } else {
                Log.d("infoAA", "permission to ask");
                //you don't have the permission, let's ask for it
                activityResultLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        binding.buttonCloseCamera.setOnClickListener(view -> {
            cameraProvider.unbindAll();
            binding.cameraPreview.setVisibility(View.INVISIBLE);
        });

        binding.imageButtonChangeCamera.setOnClickListener(view -> {
            if (cameraPosition == CameraSelector.LENS_FACING_BACK) {
                cameraPosition = CameraSelector.LENS_FACING_FRONT;
            } else {
                cameraPosition = CameraSelector.LENS_FACING_BACK;
            }
            initCameraProvider();
        });

        binding.imageButton.setOnClickListener(view -> {
            takePhoto();
        });
    }

    public void takePhoto() {

        ImageCapture.OutputFileOptions outputFileOptions = getOutput();
        imageCaptureUseCase.takePicture(outputFileOptions,
                ContextCompat.getMainExecutor(binding.getRoot().getContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(MainActivity.this, "Image saved correctly", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.d("info",exception.getMessage());
                        exception.printStackTrace();
                    }
                });


    }

    public ImageCapture.OutputFileOptions getOutput() {
        long timestamp = System.currentTimeMillis();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM);
        }

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                binding.getRoot().getContext().getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();
        return outputFileOptions;
    }

    int cameraPosition = CameraSelector.LENS_FACING_BACK;
    ProcessCameraProvider cameraProvider;
    ImageCapture imageCaptureUseCase;

    public void initCameraProvider() {
        binding.cameraPreview.setVisibility(View.VISIBLE);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(binding.getRoot().getContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                // Use case 1 -> to show the camera! at last
                Preview previewUseCase = new Preview.Builder().build();
                previewUseCase.setSurfaceProvider(binding.cameraPreview.getSurfaceProvider());

                // Use case 2 -> take photo
                imageCaptureUseCase = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // To group all the use cases
                UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                        .addUseCase(previewUseCase)
                        .addUseCase(imageCaptureUseCase)
                        .setViewPort(binding.cameraPreview.getViewPort())
                        .build();

                // To select the front or back camera
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraPosition)
                        .build();

                //set all together
                cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, useCaseGroup);

            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }, ContextCompat.getMainExecutor(binding.getRoot().getContext()));

    }
}