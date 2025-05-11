package com.example.ccat;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 视频编辑器活动
 */
public class VideoEditorActivity extends AppCompatActivity 
{
    private VideoView videoView;
    private SeekBar seekBarTrim;
    private TextView tvStartTime;
    private TextView tvEndTime;
    private TextView tvCurrentTime;
    private Button btnPlay;
    private Button btnSave;
    private ImageButton btnAddMusic;
    private ImageButton btnFilter;

    private String videoPath;
    private String videoName;
    private int videoDuration; // 毫秒
    private int startTrimPosition = 0; // 毫秒
    private int endTrimPosition; // 毫秒
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_editor);

        // 获取传递过来的视频信息
        videoPath = getIntent().getStringExtra("video_path");
        videoName = getIntent().getStringExtra("video_name");
        videoDuration = getIntent().getIntExtra("video_duration", 0);

        if (videoDuration == 0) 
        {
            // 如果没有获取到时长，通过MediaMetadataRetriever获取
            try 
            {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(videoPath);
                String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                videoDuration = Integer.parseInt(time);
                retriever.release();
            } 
            catch (Exception e) 
            {
                e.printStackTrace();
            }
        }

        // 设置结束位置为视频时长
        endTrimPosition = videoDuration;

        initViews();
        setupVideoPlayer();
        setupTrimControls();
        setupActionButtons();
    }

    private void initViews() 
    {
        videoView = findViewById(R.id.video_view);
        seekBarTrim = findViewById(R.id.seek_bar_trim);
        tvStartTime = findViewById(R.id.tv_start_time);
        tvEndTime = findViewById(R.id.tv_end_time);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        btnPlay = findViewById(R.id.btn_play);
        btnSave = findViewById(R.id.btn_save);
        btnAddMusic = findViewById(R.id.btn_add_music);
        btnFilter = findViewById(R.id.btn_filter);
    }

    private void setupVideoPlayer() 
    {
        // 设置视频源
        videoView.setVideoPath(videoPath);
        
        // 设置媒体控制器
        videoView.setOnPreparedListener(mp -> 
        {
            // 设置视频进度更新监听
            mp.setOnSeekCompleteListener(mp1 -> 
            {
                if (isPlaying) 
                {
                    videoView.start();
                }
            });
        });
        
        // 监听播放完成
        videoView.setOnCompletionListener(mp -> 
        {
            btnPlay.setText("播放");
            isPlaying = false;
        });
        
        // 准备播放
        videoView.requestFocus();
    }

    private void setupTrimControls() 
    {
        // 设置裁剪进度条
        seekBarTrim.setMax(videoDuration);
        seekBarTrim.setProgress(endTrimPosition);
        
        // 显示起始和结束时间
        updateTimeDisplay();
        
        // 设置进度条变化监听
        seekBarTrim.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() 
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
            {
                if (fromUser) 
                {
                    endTrimPosition = progress;
                    updateTimeDisplay();
                    
                    // 更新视频当前位置
                    videoView.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) 
            {
                if (isPlaying) 
                {
                    videoView.pause();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) 
            {
                if (isPlaying) 
                {
                    videoView.start();
                }
            }
        });
    }

    private void setupActionButtons() 
    {
        // 播放/暂停按钮
        btnPlay.setOnClickListener(v -> 
        {
            if (isPlaying) 
            {
                videoView.pause();
                btnPlay.setText("播放");
                isPlaying = false;
            } 
            else 
            {
                videoView.start();
                btnPlay.setText("暂停");
                isPlaying = true;
            }
        });
        
        // 保存按钮
        btnSave.setOnClickListener(v -> 
        {
            Toast.makeText(this, "正在处理视频...", Toast.LENGTH_SHORT).show();
            // 实际应用中，这里需要启动后台服务来处理视频
            // 为简化示例，这里仅显示一个提示
            saveEditedVideo();
        });
        
        // 添加音乐按钮
        btnAddMusic.setOnClickListener(v -> 
        {
            Toast.makeText(this, "音乐功能开发中...", Toast.LENGTH_SHORT).show();
            // 实际应用中，应该启动音乐选择界面
        });
        
        // 滤镜按钮
        btnFilter.setOnClickListener(v -> 
        {
            Toast.makeText(this, "滤镜功能开发中...", Toast.LENGTH_SHORT).show();
            // 实际应用中，应该显示滤镜选择面板
        });
        
        // 返回按钮
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void updateTimeDisplay() 
    {
        // 格式化并显示开始和结束时间
        tvStartTime.setText(formatTime(startTrimPosition));
        tvEndTime.setText(formatTime(endTrimPosition));
        tvCurrentTime.setText(formatTime(videoView.getCurrentPosition()));
    }

    private String formatTime(int timeMs) 
    {
        int seconds = (timeMs / 1000) % 60;
        int minutes = (timeMs / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void saveEditedVideo() 
    {
        // 在实际应用中，这里应该使用FFmpeg或MediaCodec进行视频处理
        // 为简化示例，这里仅模拟一个延迟操作
        findViewById(R.id.progress_overlay).setVisibility(View.VISIBLE);
        
        new Thread(() -> 
        {
            try 
            {
                // 模拟处理延迟
                Thread.sleep(2000);
                
                // 返回主线程更新UI
                runOnUiThread(() -> 
                {
                    findViewById(R.id.progress_overlay).setVisibility(View.GONE);
                    Toast.makeText(this, "视频保存成功！", Toast.LENGTH_SHORT).show();
                    
                    // 返回主界面
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });
            } 
            catch (InterruptedException e) 
            {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    protected void onPause() 
    {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) 
        {
            videoView.pause();
            isPlaying = false;
            btnPlay.setText("播放");
        }
    }

    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
        if (videoView != null) 
        {
            videoView.stopPlayback();
        }
    }
} 