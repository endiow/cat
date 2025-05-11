package com.example.ccat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频选择器活动
 */
public class VideoPickerActivity extends AppCompatActivity implements VideoGalleryAdapter.OnVideoClickListener 
{
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 1002;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private RecyclerView recyclerView;
    private VideoGalleryAdapter adapter;
    private List<VideoItem> videoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_picker);

        recyclerView = findViewById(R.id.recycler_video_gallery);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        
        videoList = new ArrayList<>();
        adapter = new VideoGalleryAdapter(this, videoList, this);
        recyclerView.setAdapter(adapter);

        // 检查权限
        if (checkPermissions()) 
        {
            loadVideos();
        } 
        else 
        {
            requestPermissions();
        }

        // 设置返回按钮
        findViewById(R.id.button_back).setOnClickListener(v -> finish());
    }

    private boolean checkPermissions() 
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 
        {
            // Android 11及以上版本使用MANAGE_EXTERNAL_STORAGE权限
            return Environment.isExternalStorageManager();
        } 
        else 
        {
            // Android 10及以下版本使用传统权限
            for (String permission : REQUIRED_PERMISSIONS) 
            {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) 
                {
                    return false;
                }
            }
            return true;
        }
    }

    private void requestPermissions() 
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 
        {
            // Android 11及以上，请求MANAGE_EXTERNAL_STORAGE权限
            try 
            {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE);
            } 
            catch (Exception e) 
            {
                // 如果上面的方法失败，打开通用设置页面
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE);
            }
        } 
        else 
        {
            // Android 10及以下，使用常规权限请求
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) 
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_EXTERNAL_STORAGE_REQUEST_CODE) 
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) 
            {
                // 权限已授予
                loadVideos();
            } 
            else 
            {
                // 用户拒绝了权限
                showPermissionDeniedDialog();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) 
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) 
        {
            boolean allGranted = true;
            for (int result : grantResults) 
            {
                if (result != PackageManager.PERMISSION_GRANTED) 
                {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) 
            {
                loadVideos();
            } 
            else 
            {
                showPermissionDeniedDialog();
            }
        }
    }

    private void showPermissionDeniedDialog() 
    {
        new AlertDialog.Builder(this)
                .setTitle("需要存储权限")
                .setMessage("该应用需要存储权限来访问视频文件。请在设置中授予权限。")
                .setPositiveButton("设置", (dialog, which) -> 
                {
                    // 跳转到应用设置页面
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("取消", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void loadVideos() 
    {
        // 使用ContentResolver查询媒体库中的视频
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION
        };
        String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, sortOrder)) 
        {
            if (cursor != null) 
            {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);

                videoList.clear();
                while (cursor.moveToNext()) 
                {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    String path = cursor.getString(dataColumn);
                    int duration = cursor.getInt(durationColumn);

                    Uri videoUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, Long.toString(id));
                    VideoItem videoItem = new VideoItem(id, name, path, videoUri.toString(), duration);
                    videoList.add(videoItem);
                }
                adapter.notifyDataSetChanged();
                
                if (videoList.isEmpty()) 
                {
                    Toast.makeText(this, "未找到视频文件", Toast.LENGTH_SHORT).show();
                }
            }
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            Toast.makeText(this, "加载视频失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onVideoClick(VideoItem video) 
    {
        // 将选择的视频传递给编辑器活动
        Intent intent = new Intent(this, VideoEditorActivity.class);
        intent.putExtra("video_path", video.getPath());
        intent.putExtra("video_name", video.getName());
        intent.putExtra("video_duration", (long) video.getDuration());
        startActivity(intent);
        finish();
    }
} 