package com.example.cat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cat.R;
import com.example.cat.model.Recipe;

import java.util.List;

/**
 * 菜谱列表适配器
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> 
{
    private List<Recipe> recipes;
    private OnRecipeClickListener listener;
    
    // 菜谱点击事件接口
    public interface OnRecipeClickListener 
    {
        void onRecipeClick(Recipe recipe);
    }
    
    public RecipeAdapter(List<Recipe> recipes, OnRecipeClickListener listener) 
    {
        this.recipes = recipes;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) 
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) 
    {
        Recipe recipe = recipes.get(position);
        
        holder.recipeImageView.setImageResource(recipe.getImageResId());
        holder.recipeNameTextView.setText(recipe.getName());
        holder.recipeDescTextView.setText(recipe.getDescription());
        holder.timeTextView.setText(recipe.getFormattedCookTime());
        holder.difficultyTextView.setText(recipe.getDifficulty());
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) 
            {
                listener.onRecipeClick(recipe);
            }
        });
    }
    
    @Override
    public int getItemCount() 
    {
        return recipes.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder 
    {
        ImageView recipeImageView;
        TextView recipeNameTextView;
        TextView recipeDescTextView;
        TextView timeTextView;
        TextView difficultyTextView;
        
        public ViewHolder(@NonNull View itemView) 
        {
            super(itemView);
            recipeImageView = itemView.findViewById(R.id.recipeImageView);
            recipeNameTextView = itemView.findViewById(R.id.recipeNameTextView);
            recipeDescTextView = itemView.findViewById(R.id.recipeDescTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            difficultyTextView = itemView.findViewById(R.id.difficultyTextView);
        }
    }
} 