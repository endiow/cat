package com.example.ccat.utils;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 视频处理工具类
 * 使用MediaCodec实现视频处理功能
 */
public class VideoProcessor 
{
    private static final String TAG = "VideoProcessor";
    private static final int TIMEOUT_USEC = 10000;
    private static final float DEFAULT_FRAME_RATE = 30.0f; // 默认帧率
    
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private ProgressCallback progressCallback;
    
    /**
     * 进度回调接口
     */
    public interface ProgressCallback 
    {
        void onProgress(float progress);
        void onSuccess(String outputPath);
        void onFailed(String reason);
    }
    
    public VideoProcessor(ProgressCallback callback) 
    {
        this.progressCallback = callback;
    }
    
    /**
     * 取消正在进行的处理
     */
    public void cancel() 
    {
        isCancelled.set(true);
    }
    
    /**
     * 裁剪视频
     * @param sourceFile 源视频文件
     * @param outputFile 输出视频文件
     * @param startTimeMs 开始时间(毫秒)
     * @param endTimeMs 结束时间(毫秒)
     */
    public void trimVideo(final File sourceFile, final File outputFile, final long startTimeMs, final long endTimeMs) 
    {
        // 记录参数日志
        Log.d(TAG, "开始剪辑视频: 源文件=" + sourceFile.getPath() + 
              ", 输出=" + outputFile.getPath() + 
              ", 开始=" + startTimeMs + "ms, 结束=" + endTimeMs + "ms");
        
        if (!sourceFile.exists()) 
        {
            if (progressCallback != null) 
            {
                progressCallback.onFailed("源文件不存在");
            }
            return;
        }
        
        // 确保输出目录存在
        if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs()) 
        {
            if (progressCallback != null) 
            {
                progressCallback.onFailed("无法创建输出目录");
            }
            return;
        }
        
        isCancelled.set(false);
        
        // 在子线程中处理
        new Thread(() -> 
        {
            MediaExtractor videoExtractor = null;
            MediaExtractor audioExtractor = null;
            MediaMuxer muxer = null;
            try 
            {
                // 创建MediaExtractor，从源文件提取媒体数据
                videoExtractor = new MediaExtractor();
                videoExtractor.setDataSource(sourceFile.getPath());
                
                audioExtractor = new MediaExtractor();
                audioExtractor.setDataSource(sourceFile.getPath());
                
                // 查找视频和音频轨道
                int videoTrackIndex = -1;
                int audioTrackIndex = -1;
                MediaFormat videoFormat = null;
                MediaFormat audioFormat = null;
                
                // 获取轨道数
                int trackCount = videoExtractor.getTrackCount();
                
                // 查找视频轨道
                for (int i = 0; i < trackCount; i++) 
                {
                    MediaFormat format = videoExtractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime != null && mime.startsWith("video/")) 
                    {
                        videoTrackIndex = i;
                        videoFormat = format;
                        break;
                    }
                }
                
                // 查找音频轨道
                for (int i = 0; i < trackCount; i++) 
                {
                    MediaFormat format = audioExtractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime != null && mime.startsWith("audio/")) 
                    {
                        audioTrackIndex = i;
                        audioFormat = format;
                        break;
                    }
                }
                
                if (videoTrackIndex < 0 || videoFormat == null) 
                {
                    if (progressCallback != null) 
                    {
                        progressCallback.onFailed("未找到视频轨道");
                    }
                    return;
                }
                
                Log.d(TAG, "找到视频轨道: " + videoTrackIndex + ", 音频轨道: " + 
                     (audioTrackIndex >= 0 ? audioTrackIndex : "无"));
                
                // 获取视频时长
                long duration = 0;
                try 
                {
                    if (videoFormat.containsKey(MediaFormat.KEY_DURATION)) 
                    {
                        duration = videoFormat.getLong(MediaFormat.KEY_DURATION) / 1000; // 微秒转毫秒
                    }
                } 
                catch (Exception e) 
                {
                    Log.e(TAG, "获取视频时长失败", e);
                }
                
                Log.d(TAG, "视频时长: " + duration + "ms");
                
                // 安全获取帧率
                float frameRate = DEFAULT_FRAME_RATE; // 默认值
                try 
                {
                    if (videoFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) 
                    {
                        // 尝试以浮点数方式获取
                        try 
                        {
                            frameRate = videoFormat.getFloat(MediaFormat.KEY_FRAME_RATE);
                        } 
                        catch (ClassCastException e) 
                        {
                            // 如果类型不匹配，尝试以整数方式获取再转为浮点数
                            frameRate = (float) videoFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
                        }
                    }
                } 
                catch (Exception e) 
                {
                    Log.w(TAG, "无法获取帧率，使用默认值：" + DEFAULT_FRAME_RATE, e);
                }
                
                // 确保时间范围合法
                long actualStartMs = Math.max(0L, startTimeMs);
                long actualEndMs = Math.min(duration > 0 ? duration : endTimeMs, endTimeMs);
                
                Log.d(TAG, "调整后时间范围: 开始=" + actualStartMs + "ms, 结束=" + actualEndMs + "ms");
                
                if (actualStartMs >= actualEndMs) 
                {
                    if (progressCallback != null) 
                    {
                        progressCallback.onFailed("无效的时间范围: " + actualStartMs + " >= " + actualEndMs);
                    }
                    return;
                }
                
                // 创建MediaMuxer，用于输出
                muxer = new MediaMuxer(outputFile.getPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                
                // 添加轨道到muxer
                videoExtractor.selectTrack(videoTrackIndex);
                int outputVideoTrackIndex = muxer.addTrack(videoFormat);
                
                // 添加音频轨道(如果有)
                int outputAudioTrackIndex = -1;
                if (audioTrackIndex >= 0 && audioFormat != null) 
                {
                    audioExtractor.selectTrack(audioTrackIndex);
                    outputAudioTrackIndex = muxer.addTrack(audioFormat);
                }
                
                // 开始合成
                muxer.start();
                
                // 计算目标时长
                long targetDurationMs = actualEndMs - actualStartMs;
                
                // 处理视频轨道
                processTrack(videoExtractor, muxer, outputVideoTrackIndex, 
                           actualStartMs, actualEndMs, targetDurationMs, true);
                
                // 处理音频轨道(如果有)
                if (outputAudioTrackIndex >= 0) 
                {
                    processTrack(audioExtractor, muxer, outputAudioTrackIndex, 
                               actualStartMs, actualEndMs, targetDurationMs, false);
                }
                
                // 处理完成
                if (!isCancelled.get()) 
                {
                    if (progressCallback != null) 
                    {
                        progressCallback.onSuccess(outputFile.getPath());
                    }
                    Log.d(TAG, "视频裁剪完成: " + outputFile.getPath());
                } 
                else 
                {
                    if (progressCallback != null) 
                    {
                        progressCallback.onFailed("处理被取消");
                    }
                    
                    // 删除未完成的文件
                    outputFile.delete();
                }
            } 
            catch (Exception e) 
            {
                Log.e(TAG, "视频裁剪失败", e);
                if (progressCallback != null) 
                {
                    progressCallback.onFailed("处理失败: " + e.getMessage());
                }
                
                // 删除未完成的文件
                if (outputFile.exists()) 
                {
                    outputFile.delete();
                }
            } 
            finally 
            {
                // 释放资源
                if (videoExtractor != null) 
                {
                    videoExtractor.release();
                }
                
                if (audioExtractor != null) 
                {
                    audioExtractor.release();
                }
                
                if (muxer != null) 
                {
                    try 
                    {
                        muxer.stop();
                        muxer.release();
                    } 
                    catch (Exception e) 
                    {
                        Log.e(TAG, "释放Muxer失败", e);
                    }
                }
            }
        }).start();
    }
    
    /**
     * 处理单个媒体轨道(视频或音频)
     */
    private void processTrack(MediaExtractor extractor, MediaMuxer muxer, 
                            int outputTrackIndex, long startTimeMs, long endTimeMs, 
                            long targetDurationMs, boolean isVideo) throws IOException 
    {
        // 创建ByteBuffer用于存放数据
        int maxBufferSize = 1024 * 1024; // 默认1MB
        
        // 将提取器定位到起始时间附近(微秒)
        long startTimeUs = startTimeMs * 1000;
        extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        
        // 获取实际的开始时间
        long actualStartTimeUs = extractor.getSampleTime();
        
        // 如果不是视频轨道或者起始时间早于请求时间，进一步调整
        if (!isVideo || actualStartTimeUs < startTimeUs) 
        {
            while (actualStartTimeUs < startTimeUs) 
            {
                // 前进到下一个样本
                if (!extractor.advance()) 
                {
                    // 如果没有更多样本，退出循环
                    break;
                }
                actualStartTimeUs = extractor.getSampleTime();
            }
        }
        
        Log.d(TAG, (isVideo ? "视频" : "音频") + " 实际起始时间: " + (actualStartTimeUs / 1000) + "ms");
        
        // 如果是音频轨道，需要进一步前进到起始时间，避免音画不同步
        if (!isVideo) 
        {
            while (extractor.getSampleTime() < startTimeUs) 
            {
                if (!extractor.advance()) 
                {
                    break;
                }
            }
        }
        
        // 计算时间基准，用于调整输出时间戳
        long baseTimeUs = extractor.getSampleTime();
        
        // 处理轨道数据
        ByteBuffer buffer = ByteBuffer.allocate(maxBufferSize);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        
        boolean sawEOS = false;
        long lastProgressReportTime = System.currentTimeMillis();
        
        while (!sawEOS && !isCancelled.get()) 
        {
            // 清空缓冲区
            buffer.clear();
            
            // 读取下一个样本
            int sampleSize = extractor.readSampleData(buffer, 0);
            
            if (sampleSize < 0) 
            {
                // 没有更多样本
                sawEOS = true;
                break;
            }
            
            // 获取当前样本时间
            long sampleTimeUs = extractor.getSampleTime();
            long sampleTimeMs = sampleTimeUs / 1000;
            
            // 检查是否超出范围
            if (sampleTimeMs > endTimeMs) 
            {
                sawEOS = true;
                break;
            }
            
            // 调整输出时间戳
            long presentationTimeUs = sampleTimeUs - baseTimeUs;
            
            // 填充缓冲区信息
            bufferInfo.offset = 0;
            bufferInfo.size = sampleSize;
            bufferInfo.presentationTimeUs = presentationTimeUs;
            bufferInfo.flags = extractor.getSampleFlags();
            
            // 写入数据到muxer
            muxer.writeSampleData(outputTrackIndex, buffer, bufferInfo);
            
            // 前进到下一个样本
            extractor.advance();
            
            // 只对视频轨道更新进度(避免重复计算)
            if (isVideo) 
            {
                // 计算进度
                float progress = (float)(sampleTimeMs - startTimeMs) / targetDurationMs;
                progress = Math.min(1.0f, Math.max(0.0f, progress));
                
                // 限制进度更新频率，减少UI负担
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastProgressReportTime > 100) 
                { // 至少间隔100毫秒
                    lastProgressReportTime = currentTime;
                    if (progressCallback != null) 
                    {
                        progressCallback.onProgress(progress);
                    }
                }
            }
        }
    }
    
    /**
     * 应用滤镜效果（基础实现）
     */
    public void applyFilter(File sourceFile, File outputFile, String filterType) 
    {
        // TODO: 实现滤镜效果
        // 这需要使用OpenGL和MediaCodec结合的方式，比较复杂
        if (progressCallback != null) 
        {
            progressCallback.onFailed("滤镜功能尚未实现");
        }
    }
    
    /**
     * 提取视频缩略图
     */
    public void extractThumbnail(File sourceFile, File outputFile, long timeMs) 
    {
        // TODO: 提取指定时间的视频帧作为缩略图
        if (progressCallback != null) 
        {
            progressCallback.onFailed("缩略图提取功能尚未实现");
        }
    }
} 