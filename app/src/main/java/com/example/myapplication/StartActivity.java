package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start); // 设置内容视图为 activity_main.xml

        // 找到按钮控件
        View btnStartDetection = findViewById(R.id.btn_start_detection);

        // 设置按钮点击事件监听器
        btnStartDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 当按钮被点击时执行的动作
                Toast.makeText(StartActivity.this, "开始检测", Toast.LENGTH_SHORT).show();

                // 这里可以添加更多的逻辑，比如启动另一个Activity、显示对话框等
            }
        });
    }
}
