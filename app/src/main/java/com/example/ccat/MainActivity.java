package com.example.ccat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ListView projectListView;
    private Button newProjectButton;
    private ArrayList<VideoProject> projectList;
    private ProjectAdapter projectAdapter;

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
        
        // 初始化视图
        initViews();
        
        // 设置事件监听器
        setupListeners();
        
        // 加载项目列表
        loadProjects();
    }
    
    private void initViews() 
    {
        projectListView = findViewById(R.id.project_list_view);
        newProjectButton = findViewById(R.id.new_project_button);
        
        // 初始化项目列表
        projectList = new ArrayList<>();
        projectAdapter = new ProjectAdapter(this, projectList);
        projectListView.setAdapter(projectAdapter);
    }
    
    private void setupListeners() 
    {
        // 新建项目按钮点击事件
        newProjectButton.setOnClickListener(new View.OnClickListener() 
        {
            @Override
            public void onClick(View v) 
            {
                // 启动视频选择活动
                Intent intent = new Intent(MainActivity.this, VideoPickerActivity.class);
                startActivity(intent);
            }
        });
        
        // 项目列表点击事件
        projectListView.setOnItemClickListener((parent, view, position, id) -> 
        {
            VideoProject project = projectList.get(position);
            Intent intent = new Intent(MainActivity.this, VideoEditorActivity.class);
            intent.putExtra("project_id", project.getId());
            startActivity(intent);
        });
    }
    
    private void loadProjects() 
    {
        // 从存储加载项目列表
        // 这里只是演示，实际应从数据库或文件系统加载
        projectList.clear();
        projectList.add(new VideoProject(1, "示例项目", System.currentTimeMillis()));
        projectAdapter.notifyDataSetChanged();
    }
    
    @Override
    protected void onResume() 
    {
        super.onResume();
        // 每次回到主页面时刷新项目列表
        loadProjects();
    }
}