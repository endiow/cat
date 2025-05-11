package com.example.ccat;

/**
 * 视频条目模型类
 */
public class VideoItem 
{
    private long id;
    private String name;
    private String path;
    private String uri;
    private long duration; // 单位：毫秒，改为long类型

    public VideoItem(long id, String name, String path, String uri, long duration) 
    {
        this.id = id;
        this.name = name;
        this.path = path;
        this.uri = uri;
        this.duration = duration;
    }

    public long getId() 
    {
        return id;
    }

    public String getName() 
    {
        return name;
    }

    public String getPath() 
    {
        return path;
    }

    public String getUri() 
    {
        return uri;
    }

    public long getDuration() 
    {
        return duration;
    }

    /**
     * 获取格式化的时长字符串 (mm:ss)
     */
    public String getFormattedDuration() 
    {
        int seconds = (int) (duration / 1000) % 60;
        int minutes = (int) ((duration / (1000 * 60)) % 60);
        int hours = (int) ((duration / (1000 * 60 * 60)) % 24);

        if (hours > 0) 
        {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } 
        else 
        {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
} 