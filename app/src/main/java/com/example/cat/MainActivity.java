package com.example.cat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cat.adapter.RecipeAdapter;
import com.example.cat.model.Recipe;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private List<Recipe> recipeList = new ArrayList<>();
    private RecipeAdapter recipeAdapter;
    private Uri capturedImageUri;
    private ImageView previewImage;
    
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    capturedImageUri = result.getData().getData();
                    if (capturedImageUri != null) {
                        // 显示拍摄或从相册选择的照片
                        if (previewImage != null) {
                            previewImage.setImageURI(capturedImageUri);
                            previewImage.setVisibility(View.VISIBLE);
                        }
                        
                        // 这里可以进一步处理图片，比如进行食物识别
                        Toast.makeText(MainActivity.this, 
                                "图片已获取，食材识别功能即将开发", 
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // 初始化预览图片控件
        previewImage = findViewById(R.id.previewImage);
        
        setupRecipeList();
        setupClickListeners();
    }
    
    private void setupRecipeList() 
    {
        // 添加示例数据
        loadSampleRecipes();
        
        // 设置RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recipesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // 创建并设置适配器
        recipeAdapter = new RecipeAdapter(recipeList, recipe -> 
        {
            // 点击菜谱事件
            Toast.makeText(MainActivity.this, 
                    "已选择: " + recipe.getName(), 
                    Toast.LENGTH_SHORT).show();
            
            // 这里可以跳转到菜谱详情页
            // Intent intent = new Intent(MainActivity.this, RecipeDetailActivity.class);
            // intent.putExtra("recipe_id", recipe.getId());
            // startActivity(intent);
        });
        
        recyclerView.setAdapter(recipeAdapter);
    }
    
    private void setupClickListeners() 
    {
        // 设置拍照按钮事件
        findViewById(R.id.takePictureButton).setOnClickListener(v -> 
        {
            // 启动相机活动
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            cameraLauncher.launch(intent);
        });
        
        // 设置预览图片点击事件
        if (previewImage != null) 
        {
            previewImage.setOnClickListener(v -> 
            {
                if (capturedImageUri != null) 
                {
                    // 显示大图或进行食材识别
                    Toast.makeText(this, "识别此图片中的食材...", Toast.LENGTH_SHORT).show();
                    // 这里可以启动食材识别活动
                    // Intent intent = new Intent(MainActivity.this, FoodRecognitionActivity.class);
                    // intent.putExtra("image_uri", capturedImageUri.toString());
                    // startActivity(intent);
                }
            });
        }
        
        // 设置搜索框事件
        findViewById(R.id.searchEditText).setOnFocusChangeListener((v, hasFocus) -> 
        {
            if (hasFocus) 
            {
                Toast.makeText(this, "搜索功能即将开发", Toast.LENGTH_SHORT).show();
                v.clearFocus();
            }
        });
        
        // 设置查看更多事件
        findViewById(R.id.viewMoreTextView).setOnClickListener(v -> 
        {
            Toast.makeText(this, "更多菜谱即将加载", Toast.LENGTH_SHORT).show();
        });
    }
    
    // 处理旧版本的Activity结果回调（备用方法）
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) 
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) 
        {
            capturedImageUri = data.getData();
            if (capturedImageUri != null) 
            {
                if (previewImage != null) 
                {
                    previewImage.setImageURI(capturedImageUri);
                    previewImage.setVisibility(View.VISIBLE);
                }
                Toast.makeText(this, "图片已获取，食材识别功能即将开发", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void loadSampleRecipes() 
    {
        recipeList.add(new Recipe(
                1,
                "西红柿炒鸡蛋",
                "家常美味，色香味俱全的经典快手菜",
                R.drawable.ic_launcher_background,
                15,
                "简单",
                4.5f
        ));
        
        recipeList.add(new Recipe(
                2,
                "红烧排骨",
                "肉质鲜嫩，口感香甜，入口即化",
                R.drawable.ic_launcher_background,
                45,
                "中等",
                4.7f
        ));
        
        recipeList.add(new Recipe(
                3,
                "鱼香肉丝",
                "川菜经典，酸甜可口，下饭神器",
                R.drawable.ic_launcher_background,
                30,
                "中等",
                4.6f
        ));
        
        recipeList.add(new Recipe(
                4,
                "宫保鸡丁",
                "麻辣鲜香，鸡肉嫩滑，花生酥脆",
                R.drawable.ic_launcher_background,
                25,
                "中等",
                4.8f
        ));
        
        recipeList.add(new Recipe(
                5,
                "蒜蓉粉丝蒸虾",
                "鲜美多汁，虾肉鲜嫩，蒜香浓郁",
                R.drawable.ic_launcher_background,
                20,
                "简单",
                4.9f
        ));
    }
}