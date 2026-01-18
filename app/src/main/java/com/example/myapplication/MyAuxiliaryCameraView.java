package com.example.myapplication;

import androidx.camera.lifecycle.ProcessCameraProvider;

import com.wuzhengai.examination.auxiliaryTools.AuxiliaryCameraView;

import android.content.Context;
import android.util.AttributeSet;

public class MyAuxiliaryCameraView extends AuxiliaryCameraView {

    public MyAuxiliaryCameraView(Context context) {
        super(context);
    }

    public MyAuxiliaryCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyAuxiliaryCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // 暴露 public 方法来访问 protected 的 getCameraProvider()
    public ProcessCameraProvider getProvider() {
        return getCameraProvider();
    }
}