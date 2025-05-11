package com.example.ccat;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.util.ArrayList;

/**
 * 视频项目列表适配器
 */
public class ProjectAdapter extends ArrayAdapter<VideoProject> 
{
    private Context context;
    private ArrayList<VideoProject> projectList;

    public ProjectAdapter(Context context, ArrayList<VideoProject> projects) 
    {
        super(context, 0, projects);
        this.context = context;
        this.projectList = projects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) 
    {
        if (convertView == null) 
        {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_project, parent, false);
        }

        VideoProject project = getItem(position);
        if (project != null) 
        {
            ImageView thumbnailView = convertView.findViewById(R.id.project_thumbnail);
            TextView nameView = convertView.findViewById(R.id.project_name);
            TextView timeView = convertView.findViewById(R.id.project_created_time);

            // 设置项目名称
            nameView.setText(project.getName());
            
            // 设置创建时间，格式化为相对时间（如"3小时前"）
            String relativeTime = DateUtils.getRelativeTimeSpanString(
                    project.getCreatedTime(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            ).toString();
            timeView.setText(relativeTime);
            
            // 设置缩略图（如果有）
            if (project.getThumbnailPath() != null && !project.getThumbnailPath().isEmpty()) 
            {
                // 使用Glide加载缩略图
                RequestOptions options = new RequestOptions()
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_video_placeholder);
                
                Glide.with(context)
                     .load(new File(project.getThumbnailPath()))
                     .apply(options)
                     .into(thumbnailView);
            }
            else 
            {
                // 使用默认图标
                thumbnailView.setImageResource(R.drawable.ic_video_placeholder);
            }
        }

        return convertView;
    }
} 