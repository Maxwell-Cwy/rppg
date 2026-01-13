package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;

import com.wuzhengai.examination.validate.CVInitialize;
import com.wuzhengai.examination.validate.CVInitializeLisener;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2秒

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 隐藏状态栏（全屏）
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        // 隐藏 ActionBar（如果主题不是 NoActionBar）
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_splash);
            CVInitialize.init(this, "ene1z0qdjnq", "e1586808310344c69e98c58bd80c9d94", new CVInitializeLisener() {
            @Override
            public void onSurcess() {
                Log.i("splash", "init CV is success.");
            }

            @Override
            public void onError(int i, String s, Exception e) {
                Log.e("splash", "init CV is fail, code is " + i + ", message is " + s);
            }
        });

        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, StartActivity.class));
            finish();
        }, SPLASH_DELAY);
    }
}