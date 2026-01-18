package com.example.myapplication;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;

import com.wuzhengai.examination.validate.CVInitialize;
import com.wuzhengai.examination.validate.CVInitializeLisener;

import com.wuzhengai.examination.auxiliaryTools.AuxiliaryCameraView;

import java.security.MessageDigest;

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

        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_SIGNATURES
            );
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA1");
                md.update(signature.toByteArray());
                String sha1 = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
                Log.d("Current SHA1", sha1); // 查看 Logcat
                // 也可以用冒号格式（更易读）：
                String sha1Formatted = android.util.Log.getStackTraceString(new Throwable()); // 不推荐
                // 更好的格式化方式：
                StringBuilder sb = new StringBuilder();
                for (byte b : md.digest()) {
                    sb.append(String.format("%02X:", b));
                }
                if (sb.length() > 0) sb.setLength(sb.length() - 1);
                Log.d("Current SHA1 (formatted)", sb.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, StartActivity.class));
            finish();
        }, SPLASH_DELAY);
    }
}