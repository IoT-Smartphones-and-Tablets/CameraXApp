package com.example.cameraxapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
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
                Log.d("infoAA","permission granted");
                //we open the camera
                initCameraProvider();
            } else {
                Log.d("infoAA","permission to ask");
                //you don't have the permission, let's ask for it
                activityResultLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        binding.buttonCloseCamera.setOnClickListener(view -> {
            cameraProvider.unbindAll();
            binding.cameraPreview.setVisibility(View.INVISIBLE);
        });

        binding.imageButtonChangeCamera.setOnClickListener(view ->{
            if(cameraPosition == CameraSelector.LENS_FACING_BACK){
                cameraPosition = CameraSelector.LENS_FACING_FRONT;
            }else{
                cameraPosition = CameraSelector.LENS_FACING_BACK;
            }
            initCameraProvider();
        });

    }

    int cameraPosition = CameraSelector.LENS_FACING_BACK;
    ProcessCameraProvider cameraProvider;

    public void initCameraProvider() {
        binding.cameraPreview.setVisibility(View.VISIBLE);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(binding.getRoot().getContext());
        cameraProviderFuture.addListener(()-> {
            try {
                cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                // Use case 1 -> to show the camera! at last
                Preview previewUseCase = new Preview.Builder().build();
                previewUseCase.setSurfaceProvider(binding.cameraPreview.getSurfaceProvider());

                // To group all the use cases
                UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                        .addUseCase(previewUseCase)
                        .setViewPort(binding.cameraPreview.getViewPort())
                        .build();

                // To select the front or back camera
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraPosition)
                        .build();

                //set all together
                cameraProvider.bindToLifecycle(MainActivity.this,cameraSelector,useCaseGroup);

            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        },ContextCompat.getMainExecutor(binding.getRoot().getContext()));

    }
}