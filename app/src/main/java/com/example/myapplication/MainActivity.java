package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.myapplication.model.DetectionTimeStamp;
import com.example.myapplication.model.OximeterData;
import com.example.myapplication.utils.DataSaver;
import com.example.myapplication.utils.TimeUtils;

import androidx.camera.view.PreviewView;
import com.google.android.material.button.MaterialButton;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
        implements BluetoothService.BluetoothListener,
        VideoRecorder.VideoListener,
        DataUploadService.UploadListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_ALL_PERMISSIONS = 1001;
    private static final int REQUEST_SELECT_DEVICE = 1002;

    // UI
    private PreviewView previewView;
    private TextView tvStatus;
    private TextView tvUploadProgress;
    private ProgressBar progressUpload;
    private MaterialButton btnBluetoothDetect;
    private MaterialButton btnStartDetection;
    private MaterialButton btnManualUpload;
    private MaterialButton btnExitPreview;

    // 服务
    private BluetoothService bluetoothService;
    private VideoRecorder videoRecorder;
    private DataUploadService uploadService;

    // 数据
    private DetectionTimeStamp timeStamp;
    private OximeterData oximeterData;
    private String videoFilePath;

    private boolean isDetectionInProgress = false;
    private boolean shouldSaveAndUpload = false;

    private final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initServices();
        checkAllPermissions();
        bindEvents();
    }

    private void initViews() {
        previewView = findViewById(R.id.preview_view);
        tvStatus = findViewById(R.id.tv_status);
        tvUploadProgress = findViewById(R.id.tv_upload_progress);
        progressUpload = findViewById(R.id.progress_upload);
        btnBluetoothDetect = findViewById(R.id.btn_bluetooth_detect);
        btnStartDetection = findViewById(R.id.btn_start_detection);
        btnManualUpload = findViewById(R.id.btn_manual_upload);
        btnExitPreview = findViewById(R.id.btn_exit_preview);

        previewView.setVisibility(View.GONE);
        btnExitPreview.setVisibility(View.GONE);
        tvStatus.setText("请点击「蓝牙检测」按钮选择设备\n当前状态：蓝牙未连接");
        btnManualUpload.setEnabled(false);
        btnManualUpload.setAlpha(0.6f);

        // 设置返回按钮点击事件
        btnExitPreview.setOnClickListener(v -> stopDetectionEarly());
    }

    private void stopDetectionEarly() {
        isDetectionInProgress = false;
        // 1. 停止视频录制
        if (videoRecorder != null) {
            videoRecorder.stopRecording(); // 确保 VideoRecorder 有 stopRecording() 方法
        }

        // 2. 停止蓝牙采集
        if (bluetoothService != null) {
            bluetoothService.stopReceivingData();
        }

        // 3. 切回控制界面
        previewView.setVisibility(View.GONE);
        btnExitPreview.setVisibility(View.GONE);
        findViewById(R.id.layout_control).setVisibility(View.VISIBLE);

        // 4. 更新状态（可选）
        runOnUiThread(() -> {
            tvStatus.append("\n⚠️ 用户提前终止检测");
            shouldSaveAndUpload = false;
            // 注意：此时没有完成90秒，所以不触发上传或保存
            // 如果需要保存部分数据，请在此处调用 DataSaver
        });
    }
    private void initServices() {
        bluetoothService = new BluetoothService(this, this);
        videoRecorder = new VideoRecorder(this, this, previewView);
        uploadService = new DataUploadService(this);
        timeStamp = new DetectionTimeStamp();
        oximeterData = new OximeterData();
    }

    private void bindEvents() {

        btnBluetoothDetect.setOnClickListener(v -> {
            startActivityForResult(new Intent(this, DeviceListActivity.class), REQUEST_SELECT_DEVICE);
        });

        btnStartDetection.setOnClickListener(v -> startDetection());

        btnManualUpload.setOnClickListener(v -> {
            if (videoFilePath != null && oximeterData.hasData()) {
                uploadService.uploadAllData(oximeterData, videoFilePath, timeStamp, this);
            } else {
                Toast.makeText(this, "无完整数据可上传", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startDetection() {
        // 标记检测开始
        isDetectionInProgress = true;
        shouldSaveAndUpload = true;
        // === 切换到全屏预览模式 ===
        previewView.setVisibility(View.VISIBLE);
        btnExitPreview.setVisibility(View.VISIBLE);
        findViewById(R.id.layout_control).setVisibility(View.GONE);

        // 1. 启动视频录制（CameraX 会自动绑定到 previewView）
        videoRecorder.startRecording(90_000); // 严格90秒

        // 2. 启动蓝牙数据采集（延迟10ms）
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            bluetoothService.startReceivingData();
        }, 10);
    }

    // ===================== BluetoothListener =====================
    @Override
    public void onBluetoothConnected(String deviceName, String deviceAddress) {
        runOnUiThread(() -> {
            btnStartDetection.setEnabled(true);
            btnStartDetection.setAlpha(1.0f);
            timeStamp.setBluetoothConnectTime(TimeUtils.getPreciseTimeStamp());
            tvStatus.append("\n蓝牙已连接：" + deviceName);
        });
    }

    @Override
    public void onBluetoothConnectFailed(String errorMsg) {
        runOnUiThread(() -> Toast.makeText(this, "连接失败：" + errorMsg, Toast.LENGTH_LONG).show());
    }

    @Override
    public void onBluetoothDisconnected() {
        if (isDetectionInProgress) {
            isDetectionInProgress = false; // 立即标记为已终止
            shouldSaveAndUpload = false;

            // 停止视频和蓝牙
            if (videoRecorder != null) {
                videoRecorder.stopRecording();
            }
            // bluetoothService 已断开，但可显式清理
            if (bluetoothService != null) {
                bluetoothService.stopReceivingData();
            }

            // 切回主界面（不保存、不上传）
            runOnUiThread(() -> {
                previewView.setVisibility(View.GONE);
                btnExitPreview.setVisibility(View.GONE);
                findViewById(R.id.layout_control).setVisibility(View.VISIBLE);

                tvStatus.append("\n❌ 蓝牙连接意外断开，检测已终止");
                Toast.makeText(this, "蓝牙断开，检测已取消", Toast.LENGTH_LONG).show();
            });
        }
    }

    @Override
    public void onDataReceived(String hexData) {
        oximeterData.addData(hexData);
    }

    @Override
    public void onDataStartReceiving(String startTime) {
        timeStamp.setBluetoothDataStartTime(startTime);
        runOnUiThread(() -> tvStatus.append("\n蓝牙数据开始采集：" + startTime));
    }

    @Override
    public void onDataStopReceiving(String endTime) {
        timeStamp.setBluetoothDataEndTime(endTime);
        runOnUiThread(() -> tvStatus.append("\n蓝牙数据结束采集：" + endTime));
    }

    // ===================== VideoListener =====================
    @Override
    public void onVideoStarted(String videoPath, String startTime) {
        videoFilePath = videoPath;
        timeStamp.setVideoStartTime(startTime);
        runOnUiThread(() -> {
            previewView.setVisibility(android.view.View.VISIBLE);
            tvStatus.append("\n视频开始录制：" + startTime);
        });
    }

    @Override
    public void onVideoFinished(String videoPath, String endTime) {
        isDetectionInProgress = false;
        timeStamp.setVideoEndTime(endTime);
        bluetoothService.stopReceivingData(); // 同步停止蓝牙

        runOnUiThread(() -> {

            // === 恢复原界面 ===
            previewView.setVisibility(View.GONE);
            btnExitPreview.setVisibility(View.GONE);
            findViewById(R.id.layout_control).setVisibility(View.VISIBLE);

            previewView.setVisibility(android.view.View.GONE);
            tvStatus.append("\n视频录制完成：" + endTime);
            tvStatus.append("\n\n检测已完成！\n" + timeStamp.toString());

            // 自动上传 + 启用手动上传
            btnManualUpload.setEnabled(true);
            btnManualUpload.setAlpha(1.0f);

            // 本地保存
            // ✅ 只有 shouldSaveAndUpload 为 true 才保存和上传！
            if (shouldSaveAndUpload) {
                tvStatus.append("\n\n检测已完成！\n" + timeStamp.toString());

                btnManualUpload.setEnabled(true);
                btnManualUpload.setAlpha(1.0f);

                try {
                    DataSaver.saveAllData(this, videoPath, oximeterData, timeStamp);
                    tvStatus.append("\n本地保存成功");
                } catch (Exception e) {
                    tvStatus.append("\n本地保存失败：" + e.getMessage());
                }

                uploadService.uploadAllData(oximeterData, videoPath, timeStamp, this);
            } else {
                // ❌ 被提前终止（蓝牙断开 / 用户退出），不保存
                tvStatus.append("\n⚠️ 检测未正常完成，数据已丢弃");
                Toast.makeText(this, "检测未完成，数据未保存", Toast.LENGTH_SHORT).show();
            }
            // 自动上传
            uploadService.uploadAllData(oximeterData, videoPath, timeStamp, this);
        });
    }

    @Override
    public void onVideoError(String errorMsg) {
        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle("视频录制失败")
                    .setMessage(errorMsg)
                    .setPositiveButton("重试", (d, w) -> startDetection())
                    .show();
        });
    }

    // ===================== UploadListener =====================
    @Override
    public void onUploadSuccess(String response) {
        runOnUiThread(() -> {
            hideUploadProgress();
            tvStatus.append("\n上传成功！\n服务器响应：" + response);
            Toast.makeText(this, "上传成功", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onUploadFailed(String errorMsg) {
        runOnUiThread(() -> {
            hideUploadProgress();
            tvStatus.append("\n上传失败：" + errorMsg + "\n可点击“手动上传”重试");
            Toast.makeText(this, "上传失败：" + errorMsg, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onUploadProgress(int progress) {
        runOnUiThread(() -> {
            progressUpload.setVisibility(android.view.View.VISIBLE);
            tvUploadProgress.setVisibility(android.view.View.VISIBLE);
            progressUpload.setProgress(progress);
            tvUploadProgress.setText("上传中... " + progress + "%");
        });
    }

    private void hideUploadProgress() {
        progressUpload.setVisibility(android.view.View.GONE);
        tvUploadProgress.setVisibility(android.view.View.GONE);
    }

    private void checkAllPermissions() {
        boolean missing = false;
        for (String p : REQUIRED_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                missing = true;
                break;
            }
        }
        if (missing) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_ALL_PERMISSIONS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_DEVICE && resultCode == RESULT_OK && data != null) {
            String address = data.getStringExtra("DEVICE_ADDRESS");
            bluetoothService.connectToDevice(address);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) bluetoothService.disconnect();
        if (videoRecorder != null) videoRecorder.releaseResources();
    }
}