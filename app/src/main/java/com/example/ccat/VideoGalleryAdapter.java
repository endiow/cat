package com.example.ccat;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.util.List;

/**
 * 视频库适配器
 */
public class VideoGalleryAdapter extends RecyclerView.Adapter<VideoGalleryAdapter.VideoViewHolder> 
{
    private Context context;
    private List<VideoItem> videoList;
    private OnVideoClickListener listener;

    public interface OnVideoClickListener 
    {
        void onVideoClick(VideoItem video);
    }

    public VideoGalleryAdapter(Context context, List<VideoItem> videoList, OnVideoClickListener listener) 
    {
        this.context = context;
        this.videoList = videoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) 
    {
        View view = LayoutInflater.from(context).inflate(R.layout.item_video_gallery, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) 
    {
        VideoItem video = videoList.get(position);
        
        // 设置视频名称
        holder.videoName.setText(video.getName());
        
        // 设置视频时长
        holder.videoDuration.setText(video.getFormattedDuration());
        
        // 使用Glide加载视频缩略图
        RequestOptions options = new RequestOptions()
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_video_placeholder);
        
        Glide.with(context)
             .load(new File(video.getPath()))
             .thumbnail(0.1f)
             .apply(options)
             .into(holder.videoThumbnail);
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> 
        {
            if (listener != null) 
            {
                listener.onVideoClick(video);
            }
        });
    }

    @Override
    public int getItemCount() 
    {
        return videoList.size();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder 
    {
        ImageView videoThumbnail;
        TextView videoName;
        TextView videoDuration;

        public VideoViewHolder(@NonNull View itemView) 
        {
            super(itemView);
            videoThumbnail = itemView.findViewById(R.id.video_thumbnail);
            videoName = itemView.findViewById(R.id.video_name);
            videoDuration = itemView.findViewById(R.id.video_duration);
        }
    }
} 