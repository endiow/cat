package com.example.cat.model;

/**
 * 菜谱模型类
 */
public class Recipe 
{
    private long id;
    private String name;          // 菜品名称
    private String description;   // 菜品描述
    private int imageResId;       // 图片资源ID
    private int cookTime;         // 烹饪时间（分钟）
    private String difficulty;    // 难度
    private float rating;         // 评分（1-5）
    
    public Recipe(long id, String name, String description, int imageResId, 
                 int cookTime, String difficulty, float rating) 
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageResId = imageResId;
        this.cookTime = cookTime;
        this.difficulty = difficulty;
        this.rating = rating;
    }
    
    // Getters
    public long getId() 
    {
        return id;
    }
    
    public String getName() 
    {
        return name;
    }
    
    public String getDescription() 
    {
        return description;
    }
    
    public int getImageResId() 
    {
        return imageResId;
    }
    
    public int getCookTime() 
    {
        return cookTime;
    }
    
    public String getFormattedCookTime() 
    {
        return cookTime + "分钟";
    }
    
    public String getDifficulty() 
    {
        return difficulty;
    }
    
    public float getRating() 
    {
        return rating;
    }
} 