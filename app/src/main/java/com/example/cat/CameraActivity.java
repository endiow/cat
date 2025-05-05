package com.example.cat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.example.cat.databinding.ActivityCameraBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class CameraActivity extends AppCompatActivity 
{
    private static final String TAG = "CameraActivity";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    
    private ActivityCameraBinding binding;
    private ImageCapture imageCapture;
    private Uri savedImageUri;
    
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> 
            {
                boolean allGranted = true;
                for (Boolean granted : permissions.values()) 
                {
                    allGranted = allGranted && granted;
                }
                
                if (allGranted) 
                {
                    startCamera();
                } 
                else 
                {
                    Toast.makeText(this, "需要相机权限才能使用此功能", Toast.LENGTH_LONG).show();
                    finish();
                }
            });
    
    // 定义从相册选择图片的结果处理器
    private final ActivityResultLauncher<Intent> galleryLauncher = 
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> 
            {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) 
                {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) 
                    {
                        savedImageUri = selectedImageUri;
                        showCapturedImage(savedImageUri);
                        Log.d(TAG, "从相册选择的图片: " + savedImageUri);
                    }
                }
            });
    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 检查并请求相机权限
        if (checkCameraPermissions()) 
        {
            startCamera();
        }
        
        // 设置拍照按钮点击事件
        binding.captureButton.setOnClickListener(v -> takePhoto());
        
        // 设置相册按钮点击事件
        binding.galleryButton.setOnClickListener(v -> openGallery());
        
        // 设置确认按钮点击事件
        binding.confirmButton.setOnClickListener(v -> 
        {
            if (savedImageUri != null) 
            {
                Intent resultIntent = new Intent();
                resultIntent.setData(savedImageUri);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
        
        // 设置重拍按钮点击事件
        binding.retakeButton.setOnClickListener(v -> 
        {
            binding.previewImageView.setVisibility(View.GONE);
            binding.confirmationLayout.setVisibility(View.GONE);
            binding.previewView.setVisibility(View.VISIBLE);
            binding.controlsLayout.setVisibility(View.VISIBLE);
        });
    }
    
    // 打开相册选择图片
    private void openGallery() 
    {
        // 检查是否有读取外部存储的权限
        if (checkStoragePermissions()) 
        {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        }
    }
    
    // 检查存储权限
    private boolean checkStoragePermissions() 
    {
        String[] permissions;
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) 
        {
            permissions = new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE };
        } 
        else 
        {
            permissions = new String[]{ Manifest.permission.READ_MEDIA_IMAGES };
        }
        
        boolean allPermissionsGranted = true;
        for (String permission : permissions) 
        {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) 
            {
                allPermissionsGranted = false;
                break;
            }
        }
        
        if (!allPermissionsGranted) 
        {
            requestPermissionLauncher.launch(permissions);
            return false;
        }
        
        return true;
    }
    
    private boolean checkCameraPermissions() 
    {
        String[] permissions;
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) 
        {
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        } 
        else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) 
        {
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        } 
        else 
        {
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        }
        
        boolean allPermissionsGranted = true;
        for (String permission : permissions) 
        {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) 
            {
                allPermissionsGranted = false;
                break;
            }
        }
        
        if (!allPermissionsGranted) 
        {
            requestPermissionLauncher.launch(permissions);
            return false;
        }
        
        return true;
    }
    
    private void startCamera() 
    {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(this);
                
        cameraProviderFuture.addListener(() -> 
        {
            try 
            {
                // 获取相机提供者实例
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                
                // 设置预览
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());
                
                // 设置图像捕获
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();
                
                // 选择后置相机
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                
                // 解绑所有用例
                cameraProvider.unbindAll();
                
                // 绑定用例到相机
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture);
                
            } 
            catch (ExecutionException | InterruptedException e) 
            {
                Log.e(TAG, "相机启动失败: ", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }
    
    private void takePhoto() 
    {
        if (imageCapture == null) 
        {
            Toast.makeText(this, "相机初始化中，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 创建时间戳文件名
        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.CHINA)
                .format(System.currentTimeMillis());
        
        // 创建MediaStore内容
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) 
        {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CatApp");
        }
        
        // 创建输出选项
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();
        
        // 拍照
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() 
                {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) 
                    {
                        savedImageUri = outputFileResults.getSavedUri();
                        showCapturedImage(savedImageUri);
                        Log.d(TAG, "照片保存成功: " + savedImageUri);
                    }
                    
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) 
                    {
                        Log.e(TAG, "拍照失败: ", exception);
                        Toast.makeText(CameraActivity.this, "拍照失败: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void showCapturedImage(Uri imageUri) 
    {
        binding.previewView.setVisibility(View.GONE);
        binding.controlsLayout.setVisibility(View.GONE);
        
        binding.previewImageView.setImageURI(imageUri);
        binding.previewImageView.setVisibility(View.VISIBLE);
        binding.confirmationLayout.setVisibility(View.VISIBLE);
    }
    
    public static void start(Context context, int requestCode) 
    {
        Intent intent = new Intent(context, CameraActivity.class);
        ((AppCompatActivity) context).startActivityForResult(intent, requestCode);
    }
} 