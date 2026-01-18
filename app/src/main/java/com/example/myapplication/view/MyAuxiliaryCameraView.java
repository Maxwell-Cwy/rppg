package com.example.myapplication.view;

import static androidx.camera.video.Quality.HD;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraFilter;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.wuzhengai.examination.auxiliaryTools.AuxiliaryCore;
import com.wuzhengai.examination.auxiliaryTools.MyCameraFilter;
import com.wuzhengai.examination.utils.ConfigUtils;
import com.wuzhengai.examination.utils.LogUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MyAuxiliaryCameraView extends RelativeLayout {
    private static final String TAG = "MyAuxiliaryCameraView";
    AuxiliaryCore auxiliaryCore;
    private double RATIO_4_3_VALUE;
    private double RATIO_16_9_VALUE;
    private ProcessCameraProvider cameraProvider;
    PreviewView previewView;
    private ImageAnalysis imageAnalyzer;
    CameraFilter filter;
    boolean grayMode;
    boolean mirroMode;
    LifecycleOwner lifecycleOwner;
    boolean initSurcess;
    boolean loadCamera;
    int cameraRotation;
    Preview preview;
    ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    boolean readyEnd;
    int previewRoation;
    int startFrame;
    int waitFrame;

    public MyAuxiliaryCameraView(@NonNull Context context) {
        this(context, (AttributeSet) null);
    }

    public MyAuxiliaryCameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyAuxiliaryCameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.RATIO_4_3_VALUE = 1.3333333333333333;
        this.RATIO_16_9_VALUE = 1.7777777777777777;
        this.imageAnalyzer = null;
        this.grayMode = false;
        this.mirroMode = false;
        this.initSurcess = false;
        this.loadCamera = false;
        this.cameraRotation = 0;
        this.readyEnd = false;
        this.previewRoation = -1;
        this.startFrame = 0;
        this.waitFrame = 60;
        this.init(context);
    }

    private synchronized void init(Context context) {
        synchronized (context) {
            if (!this.isInitSurcess()) {
                this.setInitSurcess(true);
                this.previewView = new PreviewView(context);
                this.addView(this.previewView, -1, -1);
                this.previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
                if (ConfigUtils.autoInitAuxiliaryCore == 1) {
                    AuxiliaryCore core = new AuxiliaryCore();
                    this.initCore(core);
                }

                if (this.lifecycleOwner != null && !this.loadCamera) {
                    this.initCamera(this.lifecycleOwner);
                }
            }

        }
    }

    private void initCore(AuxiliaryCore workCore) {
        this.auxiliaryCore = workCore;
        this.auxiliaryCore.init(this.getContext());
    }

    private void loadCameraMessage() {
        int screenAspectRatio = this.aspectRatio(640, 640);
        this.imageAnalyzer = (new ImageAnalysis.Builder()).setTargetAspectRatio(screenAspectRatio).setOutputImageRotationEnabled(true).build();
        this.imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor(), (image) -> {
            if (this.auxiliaryCore != null && this.auxiliaryCore.isWorkReady() && !this.readyEnd && this.startFrame >= this.waitFrame) {
                this.auxiliaryCore.coreWork(image.toBitmap(), this.mirroMode, this.grayMode);
            }

            if (this.startFrame < this.waitFrame + 1) {
                ++this.startFrame;
            }

            image.close();
        });
        this.preview = (new Preview.Builder()).build();
        this.imageCapture = (new ImageCapture.Builder()).setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setTargetAspectRatio(screenAspectRatio).setTargetRotation(this.cameraRotation).build();
        this.preview.setSurfaceProvider(this.previewView.getSurfaceProvider());
        if (this.cameraRotation == 0) {
            this.previewView.setRotation(0.0F);
        } else if (this.cameraRotation == 1) {
            this.previewView.setRotation(90.0F);
        } else if (this.cameraRotation == 2) {
            this.previewView.setRotation(180.0F);
        } else if (this.cameraRotation == 3) {
            this.previewView.setRotation(270.0F);
        } else {
            this.previewView.setRotation(0.0F);
        }

        if (this.previewRoation != -1) {
            this.previewView.setRotation((float) this.previewRoation);
        }
        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(HD))
                .build();
        videoCapture = VideoCapture.withOutput(recorder);

        CameraSelector cameraSelector = (new CameraSelector.Builder()).addCameraFilter(this.filter != null ? this.filter : new MyCameraFilter()).build();
        this.cameraProvider.unbindAll();
        try {
            this.cameraProvider.bindToLifecycle(this.lifecycleOwner, cameraSelector, this.preview, this.imageCapture, this.imageAnalyzer, this.videoCapture);
        } catch (Exception var4) {
            Exception e = var4;
            e.printStackTrace();
            Log.e(TAG, "loadCameraMessage: 无法获取相机");
        }

    }

    @SuppressLint({"RestrictedApi"})
    private void bindPreview(@NonNull ProcessCameraProvider cp) {
        this.cameraProvider = cp;
        this.loadCameraMessage();
    }

    public void rebindCamera() {
        this.startFrame = 0;
        this.loadCameraMessage();
    }

    public void setFilter(CameraFilter filter) {
        this.filter = filter;
    }

    private int aspectRatio(int width, int height) {
        int max = Math.max(width, height);
        int min = Math.min(width, height);
        double previewRatio = (double) max / (double) min;
        return Math.abs(previewRatio - this.RATIO_4_3_VALUE) <= Math.abs(previewRatio - this.RATIO_16_9_VALUE) ? 0 : 1;
    }

    public void setGrayMode(boolean grayMode) {
        this.grayMode = grayMode;
    }

    public synchronized void initCamera(LifecycleOwner lifecycleOwner) {
        if (this.auxiliaryCore == null) {
            LogUtils.W("请先初始化AuxiliaryCore");
        } else {
            if (this.lifecycleOwner != lifecycleOwner) {
                this.lifecycleOwner = lifecycleOwner;
            }

            if (!this.loadCamera) {
                this.loadCamera = true;
                ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this.getContext());
                cameraProviderFuture.addListener(() -> {
                    try {
                        ProcessCameraProvider cameraProvider = (ProcessCameraProvider) cameraProviderFuture.get();
                        this.bindPreview(cameraProvider);
                    } catch (InterruptedException | ExecutionException var3) {
                        Exception e = var3;
                        e.printStackTrace();
                    }

                }, ContextCompat.getMainExecutor(this.getContext()));
            }
        }

    }

    public AuxiliaryCore getAuxiliaryCore() {
        return this.auxiliaryCore;
    }

    protected ProcessCameraProvider getCameraProvider() {
        return this.cameraProvider;
    }

    public VideoCapture<Recorder> getVideoCapture() {
        return videoCapture;
    }

    public void setCameraRotation(int cameraRotation) {
        this.cameraRotation = cameraRotation;
    }

    public void setMirroMode(boolean mirroMode) {
        this.mirroMode = mirroMode;
    }

    public void setPreviewRoation(int previewRoation) {
        this.previewRoation = previewRoation;
    }

    public void setWaitFrame(int waitFrame) {
        this.waitFrame = waitFrame;
    }

    private synchronized boolean isInitSurcess() {
        return this.initSurcess;
    }

    private synchronized void setInitSurcess(boolean initSurcess) {
        this.initSurcess = initSurcess;
    }
}
