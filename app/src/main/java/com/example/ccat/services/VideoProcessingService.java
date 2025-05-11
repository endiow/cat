package com.example.ccat.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.ccat.MainActivity;
import com.example.ccat.R;
import com.example.ccat.utils.VideoProcessor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 视频处理服务
 * 在后台执行视频处理任务，并显示通知
 */
public class VideoProcessingService extends Service 
{
    private static final String TAG = "VideoProcessingService";
    
    // Action常量
    public static final String ACTION_TRIM_VIDEO = "com.example.ccat.action.TRIM_VIDEO";
    public static final String ACTION_APPLY_FILTER = "com.example.ccat.action.APPLY_FILTER";
    public static final String ACTION_CANCEL = "com.example.ccat.action.CANCEL";
    
    // 额外参数常量
    public static final String EXTRA_SOURCE_PATH = "source_path";
    public static final String EXTRA_START_TIME = "start_time";
    public static final String EXTRA_END_TIME = "end_time";
    public static final String EXTRA_FILTER_TYPE = "filter_type";
    
    // 通知相关常量
    private static final String CHANNEL_ID = "video_processing_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    // 视频处理器
    private VideoProcessor videoProcessor;
    private boolean isProcessing = false;
    
    @Override
    public void onCreate() 
    {
        super.onCreate();
        // 创建通知渠道（Android 8.0及以上需要）
        createNotificationChannel();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) 
    {
        return null; // 不需要绑定
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) 
    {
        if (intent == null || intent.getAction() == null) 
        {
            return START_NOT_STICKY;
        }
        
        String action = intent.getAction();
        
        switch (action) 
        {
            case ACTION_TRIM_VIDEO:
                handleTrimVideo(intent);
                break;
                
            case ACTION_APPLY_FILTER:
                handleApplyFilter(intent);
                break;
                
            case ACTION_CANCEL:
                handleCancel();
                break;
        }
        
        return START_NOT_STICKY;
    }
    
    /**
     * 处理视频裁剪请求
     */
    private void handleTrimVideo(Intent intent) 
    {
        // 如果已经在处理中，直接返回
        if (isProcessing) 
        {
            Log.w(TAG, "已有处理任务在进行中，忽略新请求");
            return;
        }
        
        // 获取参数
        String sourcePath = intent.getStringExtra(EXTRA_SOURCE_PATH);
        // 安全获取Long类型值，避免类型转换异常
        long startTime = 0L;
        long endTime = 0L;
        
        try 
        {
            startTime = intent.getLongExtra(EXTRA_START_TIME, 0L);
            endTime = intent.getLongExtra(EXTRA_END_TIME, 0L);
            
            Log.d(TAG, "从Intent获取时间参数: 开始=" + startTime + "ms, 结束=" + endTime + "ms");
        } 
        catch (Exception e) 
        {
            Log.e(TAG, "获取时间参数失败", e);
            broadcastFailure("参数错误: " + e.getMessage());
            stopSelf();
            return;
        }
        
        // 确认时间参数有效
        if (startTime >= endTime) 
        {
            String errorMsg = "无效的时间范围: 开始=" + startTime + "ms, 结束=" + endTime + "ms";
            Log.e(TAG, errorMsg);
            broadcastFailure(errorMsg);
            stopSelf();
            return;
        }
        
        if (sourcePath == null) 
        {
            Log.e(TAG, "源文件路径为空");
            broadcastFailure("源文件路径为空");
            stopSelf();
            return;
        }
        
        // 检查源文件是否存在
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists() || !sourceFile.canRead()) 
        {
            String errorMsg = "源文件不存在或无法读取: " + sourcePath;
            Log.e(TAG, errorMsg);
            broadcastFailure(errorMsg);
            stopSelf();
            return;
        }
        
        // 输出调试信息
        Log.d(TAG, "准备裁剪视频: 源文件=" + sourcePath + ", 开始=" + startTime + "ms, 结束=" + endTime + "ms");
        
        // 创建输出文件
        File outputDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), "CCat");
        
        if (!outputDir.exists() && !outputDir.mkdirs()) 
        {
            String errorMsg = "无法创建输出目录: " + outputDir.getPath();
            Log.e(TAG, errorMsg);
            broadcastFailure(errorMsg);
            stopSelf();
            return;
        }
        
        // 生成唯一文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File outputFile = new File(outputDir, "TRIM_" + timeStamp + ".mp4");
        
        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification("正在裁剪视频...", 0));
        isProcessing = true;
        
        // 创建处理器并开始处理
        videoProcessor = new VideoProcessor(new VideoProcessor.ProgressCallback() 
        {
            @Override
            public void onProgress(float progress) 
            {
                // 更新通知进度
                updateNotification("正在裁剪视频...", (int) (progress * 100));
            }
            
            @Override
            public void onSuccess(String outputPath) 
            {
                // 通知处理成功
                updateNotification("视频裁剪完成", 100);
                
                // 发送广播通知应用
                broadcastSuccess(outputPath);
                
                // 结束服务
                isProcessing = false;
                stopForeground(true);
                stopSelf();
            }
            
            @Override
            public void onFailed(String reason) 
            {
                // 通知处理失败
                updateNotification("视频裁剪失败: " + reason, 0);
                
                // 发送广播通知应用
                broadcastFailure(reason);
                
                // 结束服务
                isProcessing = false;
                stopForeground(true);
                stopSelf();
            }
        });
        
        // 开始裁剪视频
        videoProcessor.trimVideo(sourceFile, outputFile, startTime, endTime);
    }
    
    /**
     * 广播处理成功消息
     */
    private void broadcastSuccess(String outputPath) 
    {
        Intent broadcastIntent = new Intent("com.example.ccat.VIDEO_PROCESSED");
        broadcastIntent.putExtra("success", true);
        broadcastIntent.putExtra("output_path", outputPath);
        sendBroadcast(broadcastIntent);
        Log.d(TAG, "发送成功广播: output_path=" + outputPath);
    }
    
    /**
     * 广播处理失败消息
     */
    private void broadcastFailure(String error) 
    {
        Intent broadcastIntent = new Intent("com.example.ccat.VIDEO_PROCESSED");
        broadcastIntent.putExtra("success", false);
        broadcastIntent.putExtra("error", error);
        sendBroadcast(broadcastIntent);
        Log.e(TAG, "发送失败广播: error=" + error);
    }
    
    /**
     * 处理应用滤镜请求
     */
    private void handleApplyFilter(Intent intent) 
    {
        // TODO: 实现应用滤镜功能
        stopSelf();
    }
    
    /**
     * 处理取消请求
     */
    private void handleCancel() 
    {
        if (videoProcessor != null) 
        {
            videoProcessor.cancel();
        }
        
        updateNotification("已取消视频处理", 0);
        
        isProcessing = false;
        stopForeground(true);
        stopSelf();
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() 
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
        {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "视频处理",
                    NotificationManager.IMPORTANCE_LOW);
            
            channel.setDescription("显示视频处理进度");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) 
            {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * 创建通知
     */
    private Notification createNotification(String text, int progress) 
    {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        
        // 创建取消意图
        Intent cancelIntent = new Intent(this, VideoProcessingService.class);
        cancelIntent.setAction(ACTION_CANCEL);
        PendingIntent cancelPendingIntent = PendingIntent.getService(
                this, 1, cancelIntent, PendingIntent.FLAG_IMMUTABLE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("视频处理")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_video_placeholder)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消", cancelPendingIntent);
        
        if (progress > 0) 
        {
            builder.setProgress(100, progress, false);
        }
        
        return builder.build();
    }
    
    /**
     * 更新通知
     */
    private void updateNotification(String text, int progress) 
    {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) 
        {
            notificationManager.notify(NOTIFICATION_ID, createNotification(text, progress));
        }
    }
    
    @Override
    public void onDestroy() 
    {
        if (videoProcessor != null) 
        {
            videoProcessor.cancel();
        }
        super.onDestroy();
    }
} 