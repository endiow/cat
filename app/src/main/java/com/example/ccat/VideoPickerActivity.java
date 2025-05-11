package com.example.ccat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
        for (String permission : REQUIRED_PERMISSIONS) 
        {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) 
            {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() 
    {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
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
                Toast.makeText(this, "需要存储权限来访问视频", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
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
        intent.putExtra("video_duration", video.getDuration());
        startActivity(intent);
        finish();
    }
} 