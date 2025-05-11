package com.example.ccat;

/**
 * 视频项目模型类
 */
public class VideoProject 
{
    private long id;
    private String name;
    private long createdTime;
    private String thumbnailPath;
    private String videoPath;
    private int duration; // 单位：毫秒

    public VideoProject(long id, String name, long createdTime) 
    {
        this.id = id;
        this.name = name;
        this.createdTime = createdTime;
    }

    public long getId() 
    {
        return id;
    }

    public void setId(long id) 
    {
        this.id = id;
    }

    public String getName() 
    {
        return name;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public long getCreatedTime() 
    {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) 
    {
        this.createdTime = createdTime;
    }

    public String getThumbnailPath() 
    {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) 
    {
        this.thumbnailPath = thumbnailPath;
    }

    public String getVideoPath() 
    {
        return videoPath;
    }

    public void setVideoPath(String videoPath) 
    {
        this.videoPath = videoPath;
    }

    public int getDuration() 
    {
        return duration;
    }

    public void setDuration(int duration) 
    {
        this.duration = duration;
    }
} 