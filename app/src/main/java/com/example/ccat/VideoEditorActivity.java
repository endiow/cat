package com.example.ccat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ccat.services.VideoProcessingService;

import java.io.File;

/**
 * 视频编辑器活动
 */
public class VideoEditorActivity extends AppCompatActivity 
{
    private static final String TAG = "VideoEditorActivity";
    
    private VideoView videoView;
    private SeekBar seekBarTrim;
    private TextView tvStartTime;
    private TextView tvEndTime;
    private TextView tvCurrentTime;
    private Button btnPlay;
    private Button btnSave;
    private ImageButton btnAddMusic;
    private ImageButton btnFilter;
    private View progressOverlay;

    private String videoPath;
    private String videoName;
    private long videoDuration; // 毫秒
    private long startTrimPosition = 0L; // 毫秒
    private long endTrimPosition; // 毫秒
    private boolean isPlaying = false;
    
    private Handler handler;
    private Runnable updateTimeRunnable;
    
    // 广播接收器，接收视频处理结果
    private BroadcastReceiver videoProcessedReceiver = new BroadcastReceiver() 
    {
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            boolean success = intent.getBooleanExtra("success", false);
            if (success) 
            {
                String outputPath = intent.getStringExtra("output_path");
                onVideoProcessingSuccess(outputPath);
            } 
            else 
            {
                String error = intent.getStringExtra("error");
                onVideoProcessingFailed(error);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_editor);

        // 初始化Handler用于定时更新时间
        handler = new Handler(Looper.getMainLooper());
        
        // 获取传递过来的视频信息
        videoPath = getIntent().getStringExtra("video_path");
        videoName = getIntent().getStringExtra("video_name");
        videoDuration = getIntent().getLongExtra("video_duration", 0);
        
        // 检查文件是否存在
        if (videoPath == null || !new File(videoPath).exists()) 
        {
            Toast.makeText(this, "视频文件不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (videoDuration == 0) 
        {
            // 如果没有获取到时长，通过MediaMetadataRetriever获取
            try 
            {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(videoPath);
                String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (time != null) 
                {
                    videoDuration = Long.parseLong(time);
                    Log.d(TAG, "从MediaMetadataRetriever获取视频时长: " + videoDuration + "ms");
                } 
                else 
                {
                    // 如果仍然无法获取时长，设置一个默认值
                    videoDuration = 60000L; // 1分钟
                    Log.w(TAG, "无法获取视频时长，使用默认值: " + videoDuration + "ms");
                }
                retriever.release();
            } 
            catch (Exception e) 
            {
                Log.e(TAG, "读取视频时长失败", e);
                // 设置默认时长
                videoDuration = 60000L; // 1分钟
            }
        }

        // 设置结束位置为视频时长
        endTrimPosition = videoDuration;
        
        Log.d(TAG, "初始化视频编辑器: 路径=" + videoPath + ", 名称=" + videoName + ", 时长=" + videoDuration + "ms");

        initViews();
        setupVideoPlayer();
        setupTrimControls();
        setupActionButtons();
        
        // 注册广播接收器
        IntentFilter filter = new IntentFilter("com.example.ccat.VIDEO_PROCESSED");
        registerReceiver(videoProcessedReceiver, filter);
        
        // 创建更新时间的Runnable
        updateTimeRunnable = new Runnable() 
        {
            @Override
            public void run() 
            {
                if (videoView != null && videoView.isPlaying()) 
                {
                    updateTimeDisplay();
                }
                handler.postDelayed(this, 500); // 每500毫秒更新一次
            }
        };
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
        progressOverlay = findViewById(R.id.progress_overlay);
    }

    private void setupVideoPlayer() 
    {
        try 
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
            
            // 设置错误监听
            videoView.setOnErrorListener((mp, what, extra) -> 
            {
                Toast.makeText(VideoEditorActivity.this, "视频播放错误", Toast.LENGTH_SHORT).show();
                return true;
            });
            
            // 准备播放
            videoView.requestFocus();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            Toast.makeText(this, "无法播放视频: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupTrimControls() 
    {
        // 设置裁剪进度条
        // 注意：SeekBar只接受int类型的最大值，所以需要转换
        // 对于超过Integer.MAX_VALUE的视频，需要考虑缩放
        int maxProgress;
        if (videoDuration > Integer.MAX_VALUE) {
            maxProgress = Integer.MAX_VALUE;
            Log.w("VideoEditorActivity", "视频时长超过了进度条最大值，将进行缩放");
        } else {
            maxProgress = (int) videoDuration;
        }
        
        seekBarTrim.setMax(maxProgress);
        seekBarTrim.setProgress((int) endTrimPosition);
        
        // 显示起始和结束时间
        updateTimeDisplay();
        
        // 增加额外的SeekBar，用于调整视频起始位置
        SeekBar seekBarStart = findViewById(R.id.seek_bar_start);
        if (seekBarStart != null) {
            seekBarStart.setMax(maxProgress);
            seekBarStart.setProgress((int) startTrimPosition);
            
            seekBarStart.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() 
            {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
                {
                    if (fromUser) 
                    {
                        // 确保起始位置不超过结束位置
                        if (progress < endTrimPosition - 500) 
                        {
                            startTrimPosition = progress;
                        } 
                        else 
                        {
                            // 如果起始位置太接近结束位置，保持至少0.5秒的间隔
                            startTrimPosition = endTrimPosition - 500;
                            seekBar.setProgress((int) startTrimPosition);
                        }
                        
                        updateTimeDisplay();
                        
                        // 更新视频当前位置
                        try 
                        {
                            // 安全处理seekTo调用 - int参数限制
                            int safePosition = (int) Math.min(Integer.MAX_VALUE, startTrimPosition);
                            videoView.seekTo(safePosition);
                            Log.d(TAG, "seekTo位置: " + safePosition + "ms");
                        } 
                        catch (Exception e) 
                        {
                            Log.e(TAG, "seekTo失败", e);
                        }
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
        
        // 设置结束位置进度条变化监听
        seekBarTrim.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() 
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
            {
                if (fromUser) 
                {
                    // 确保结束位置大于起始位置
                    if (progress > startTrimPosition + 500) 
                    {
                        endTrimPosition = progress;
                    } 
                    else 
                    {
                        // 如果结束位置太接近起始位置，保持至少0.5秒的间隔
                        endTrimPosition = startTrimPosition + 500;
                        seekBar.setProgress((int) endTrimPosition);
                    }
                    
                    updateTimeDisplay();
                    
                    // 更新视频当前位置
                    try 
                    {
                        // 安全处理seekTo调用 - int参数限制
                        int safePosition = (int) Math.min(Integer.MAX_VALUE, progress);
                        videoView.seekTo(safePosition);
                        Log.d(TAG, "seekTo位置: " + safePosition + "ms");
                    } 
                    catch (Exception e) 
                    {
                        Log.e(TAG, "seekTo失败", e);
                    }
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
            try 
            {
                if (isPlaying) 
                {
                    videoView.pause();
                    btnPlay.setText("播放");
                    isPlaying = false;
                    handler.removeCallbacks(updateTimeRunnable);
                } 
                else 
                {
                    videoView.start();
                    btnPlay.setText("暂停");
                    isPlaying = true;
                    handler.post(updateTimeRunnable);
                }
            } 
            catch (Exception e) 
            {
                e.printStackTrace();
                Toast.makeText(this, "播放操作失败", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 保存按钮
        btnSave.setOnClickListener(v -> 
        {
            showSaveOptionsDialog();
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
        
        try 
        {
            tvCurrentTime.setText(formatTime(videoView.getCurrentPosition()));
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    private String formatTime(long timeMs) 
    {
        int seconds = (int) (timeMs / 1000) % 60;
        int minutes = (int) ((timeMs / (1000 * 60)) % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 显示保存选项对话框
     */
    private void showSaveOptionsDialog() 
    {
        new AlertDialog.Builder(this)
                .setTitle("保存视频")
                .setItems(new String[]{"裁剪视频", "应用滤镜", "取消"}, (dialog, which) -> 
                {
                    switch (which) 
                    {
                        case 0: // 裁剪视频
                            trimVideo();
                            break;
                            
                        case 1: // 应用滤镜
                            Toast.makeText(this, "滤镜功能开发中...", Toast.LENGTH_SHORT).show();
                            break;
                            
                        case 2: // 取消
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }
    
    /**
     * 开始裁剪视频
     */
    private void trimVideo() 
    {
        // 确保结束时间大于起始时间且有效
        if (endTrimPosition <= startTrimPosition) 
        {
            Toast.makeText(this, "无效的时间范围：结束时间必须大于开始时间", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 确保剪切范围不是0
        if (endTrimPosition - startTrimPosition < 500) 
        {
            Toast.makeText(this, "剪切范围太短，请选择至少0.5秒的视频片段", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 确保不超出视频实际时长
        if (endTrimPosition > videoDuration) 
        {
            Toast.makeText(this, "结束时间超出视频长度", Toast.LENGTH_SHORT).show();
            return;
        }

        // 暂停播放
        if (isPlaying) 
        {
            videoView.pause();
            btnPlay.setText("播放");
            isPlaying = false;
            handler.removeCallbacks(updateTimeRunnable);
        }
        
        // 显示进度覆盖层
        progressOverlay.setVisibility(View.VISIBLE);
        
        // 日志输出调试信息
        Log.d("VideoEditorActivity", "开始裁剪视频: " + 
              "起始=" + startTrimPosition + "ms, " + 
              "结束=" + endTrimPosition + "ms, " +
              "总时长=" + videoDuration + "ms");
        
        // 启动处理服务
        Intent intent = new Intent(this, VideoProcessingService.class);
        intent.setAction(VideoProcessingService.ACTION_TRIM_VIDEO);
        intent.putExtra(VideoProcessingService.EXTRA_SOURCE_PATH, videoPath);
        intent.putExtra(VideoProcessingService.EXTRA_START_TIME, (long) startTrimPosition);
        intent.putExtra(VideoProcessingService.EXTRA_END_TIME, (long) endTrimPosition);
        startService(intent);
    }
    
    /**
     * 视频处理成功回调
     */
    private void onVideoProcessingSuccess(String outputPath) 
    {
        // 隐藏进度覆盖层
        progressOverlay.setVisibility(View.GONE);
        
        // 显示成功对话框
        new AlertDialog.Builder(this)
                .setTitle("处理成功")
                .setMessage("视频已保存到：" + outputPath)
                .setPositiveButton("返回首页", (dialog, which) -> 
                {
                    // 返回主界面
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("继续编辑", (dialog, which) -> 
                {
                    // 重新加载当前视频或加载新的处理结果
                    videoPath = outputPath;
                    setupVideoPlayer();
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }
    
    /**
     * 视频处理失败回调
     */
    private void onVideoProcessingFailed(String error) 
    {
        // 隐藏进度覆盖层
        progressOverlay.setVisibility(View.GONE);
        
        // 显示错误对话框
        new AlertDialog.Builder(this)
                .setTitle("处理失败")
                .setMessage("原因：" + error)
                .setPositiveButton("确定", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onResume() 
    {
        super.onResume();
        if (isPlaying) 
        {
            handler.post(updateTimeRunnable);
        }
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
        handler.removeCallbacks(updateTimeRunnable);
    }

    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
        if (videoView != null) 
        {
            videoView.stopPlayback();
        }
        handler.removeCallbacks(updateTimeRunnable);
        
        // 注销广播接收器
        try 
        {
            unregisterReceiver(videoProcessedReceiver);
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
} 