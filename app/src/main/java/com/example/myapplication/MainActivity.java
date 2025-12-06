package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.example.myapplication.model.DetectionTimeStamp;
import com.example.myapplication.model.OximeterData;
import androidx.camera.view.PreviewView;
import com.example.myapplication.utils.DataSaver;




/**
 * 主界面：控制整体流程（蓝牙连接→开始检测→数据上传）
 */
public class MainActivity extends AppCompatActivity implements
        BluetoothService.BluetoothListener,
        VideoRecorder.VideoListener,
        DataUploadService.UploadListener {

    // 权限请求码
    private static final int REQUEST_ALL_PERMISSIONS = 1001;
    // 设备选择请求码
    private static final int REQUEST_SELECT_DEVICE = 1002;

    // UI控件
    private Button btnBluetoothDetect;   // 蓝牙检测按钮
    private Button btnStartDetection;    // 开始检测按钮
    private TextView tvStatus;           // 状态显示
    private PreviewView previewView;    // 视频预览
    // 核心服务
    private BluetoothService bluetoothService;  // 蓝牙服务
    private VideoRecorder videoRecorder;        // 视频录制服务
    private DataUploadService uploadService;    // 数据上传服务

    // 时间戳模型（记录所有关键时间）
    private DetectionTimeStamp detectionTimeStamp;
    // 视频路径
    private String videoFilePath;

    // 所需权限列表（Android 15适配）
    private final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.BLUETOOTH_SCAN,    // 蓝牙扫描
            Manifest.permission.BLUETOOTH_CONNECT,  // 蓝牙连接
            Manifest.permission.CAMERA,             // 相机（视频录制）
            Manifest.permission.RECORD_AUDIO,       // 麦克风（视频录音）
            Manifest.permission.INTERNET,           // 网络（数据上传）
            Manifest.permission.READ_MEDIA_VIDEO    // 读取视频（上传）
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化UI控件
        initView();

        // 初始化核心服务
        initServices();

        // 检查权限
        checkAllPermissions();

        // 绑定按钮点击事件
        bindButtonEvents();
    }

    /**
     * 初始化UI控件
     */
    private void initView() {
        btnBluetoothDetect = findViewById(R.id.btn_bluetooth_detect);
        btnStartDetection = findViewById(R.id.btn_start_detection);
        tvStatus = findViewById(R.id.tv_status);
        previewView = findViewById(R.id.preview_view);   // 正确
        // 设置状态文本初始值
        tvStatus.setText("请点击「蓝牙检测」按钮选择设备\n" +
                "当前状态：蓝牙未连接");
    }

    /**
     * 初始化核心服务
     */
    private void initServices() {
        // 蓝牙服务（传入当前Activity作为回调）
        bluetoothService = new BluetoothService(this, this);
        // 视频录制服务（传入预览界面）
        videoRecorder = new VideoRecorder(this, this, previewView);
        // 数据上传服务
        uploadService = new DataUploadService(this);
        // 初始化时间戳模型
        detectionTimeStamp = new DetectionTimeStamp();
    }

    /**
     * 检查所有必要权限
     */
    private void checkAllPermissions() {
        boolean hasMissingPermission = false;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                hasMissingPermission = true;
                break;
            }
        }

        // 如果有缺失的权限，请求权限
        if (hasMissingPermission) {
            ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    REQUEST_ALL_PERMISSIONS
            );
        }
    }

    /**
     * 绑定按钮点击事件
     */
    private void bindButtonEvents() {
        // 1. 蓝牙检测按钮：跳转到设备选择界面
        btnBluetoothDetect.setOnClickListener(v -> {
            // 检查权限
            if (!isAllPermissionsGranted()) {
                Toast.makeText(this, "请先授予所有必要权限", Toast.LENGTH_SHORT).show();
                checkAllPermissions();
                return;
            }

            // 检查蓝牙是否开启
            if (!com.example.myapplication.utils.BluetoothUtils.isBluetoothEnabled()) {
                Toast.makeText(this, "请先开启蓝牙", Toast.LENGTH_SHORT).show();
                com.example.myapplication.utils.BluetoothUtils.openBluetoothSettings(this);
                return;
            }

            // 跳转到设备选择界面
            Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
            startActivityForResult(intent, REQUEST_SELECT_DEVICE);
        });

        // 2. 开始检测按钮：开始录制视频+接收蓝牙数据（默认禁用）
        btnStartDetection.setOnClickListener(v -> {
            // 检查蓝牙是否已连接
            if (!bluetoothService.isConnected()) {
                Toast.makeText(this, "蓝牙未连接，无法开始检测", Toast.LENGTH_SHORT).show();
                return;
            }

            // 检查是否正在录制
            if (videoRecorder.isRecording()) {
                Toast.makeText(this, "正在录制中，请勿重复点击", Toast.LENGTH_SHORT).show();
                return;
            }

            // 显示视频预览界面

            previewView.setVisibility(View.VISIBLE);

            // 更新状态
            tvStatus.setText("准备开始检测...\n" +
                    "蓝牙连接时间：" + detectionTimeStamp.getBluetoothConnectTime());

            // 开始录制视频（90秒）
            videoRecorder.startRecording(90000);
        });
    }

    /**
     * 检查所有权限是否已授予
     */
    private boolean isAllPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 设备选择界面返回结果
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 处理设备选择结果
        if (requestCode == REQUEST_SELECT_DEVICE && resultCode == RESULT_OK) {
            if (data != null) {
                // 获取选择的设备地址
                String deviceAddress = data.getStringExtra("DEVICE_ADDRESS");
                if (deviceAddress != null && !deviceAddress.isEmpty()) {
                    // 更新状态：正在连接
                    tvStatus.setText("正在连接设备...\n" +
                            "设备地址：" + deviceAddress);

                    // 连接蓝牙设备
                    bluetoothService.connectToDevice(deviceAddress);
                }
            }
        }
    }

    /**
     * 权限请求结果回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ALL_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                tvStatus.setText("所有权限已授予\n" +
                        "请点击「蓝牙检测」按钮选择设备");
            } else {
                tvStatus.setText("部分权限未授予\n" +
                        "功能可能无法正常使用，请在设置中授予权限");
                Toast.makeText(this, "部分权限未授予，影响功能使用", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ==================== BluetoothService.BluetoothListener 回调 ====================
    /**
     * 蓝牙连接成功
     */
    @Override
    public void onBluetoothConnected(String deviceName, String deviceAddress) {
        // 记录蓝牙连接时间戳
        String connectTime = com.example.myapplication.utils.TimeUtils.getPreciseTimeStamp();
        detectionTimeStamp.setBluetoothConnectTime(connectTime);

        // 更新状态（绿色提示连接成功）
        tvStatus.setTextColor(getResources().getColor(R.color.success_green));
        tvStatus.setText("✅ 蓝牙连接成功\n" +
                "设备名称：" + deviceName + "\n" +
                "设备地址：" + deviceAddress + "\n" +
                "连接时间：" + connectTime + "\n" +
                "可点击「开始检测」按钮开始录制");

        // 启用开始检测按钮
        btnStartDetection.setEnabled(true);
        btnStartDetection.setAlpha(1.0f);
    }

    /**
     * 蓝牙连接失败
     */
    @Override
    public void onBluetoothConnectFailed(String errorMsg) {
        // 更新状态（红色提示失败）
        tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        tvStatus.setText("❌ 蓝牙连接失败\n" +
                "原因：" + errorMsg + "\n" +
                "请重新点击「蓝牙检测」按钮");

        // 禁用开始检测按钮
        btnStartDetection.setEnabled(false);
        btnStartDetection.setAlpha(0.5f);
    }

    /**
     * 蓝牙连接断开
     */
    @Override
    public void onBluetoothDisconnected() {
        // 更新状态（红色提示断开）
        tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        tvStatus.setText("❌ 蓝牙连接已断开\n" +
                "请重新点击「蓝牙检测」按钮连接");

        // 禁用开始检测按钮
        btnStartDetection.setEnabled(false);
        btnStartDetection.setAlpha(0.5f);

        // 隐藏视频预览
        previewView.setVisibility(View.GONE);
    }

    /**
     * 蓝牙数据接收（实时显示）
     */
    @Override
    public void onDataReceived(String hexData) {
        // 实时更新数据（只显示最后一条，避免刷屏）
        String currentStatus = tvStatus.getText().toString();
        if (currentStatus.contains("实时数据：")) {
            // 替换最后一行的实时数据
            int lastIndex = currentStatus.lastIndexOf("\n");
            if (lastIndex != -1) {
                currentStatus = currentStatus.substring(0, lastIndex);
            }
        }
        tvStatus.setText(currentStatus + "\n实时数据：" + hexData);
    }

    /**
     * 蓝牙数据开始接收（记录时间戳）
     */
    @Override
    public void onDataStartReceiving() {
        // 记录数据开始时间戳
        String dataStartTime = com.example.myapplication.utils.TimeUtils.getPreciseTimeStamp();
        detectionTimeStamp.setBluetoothDataStartTime(dataStartTime);

        // 更新状态
        tvStatus.setText(tvStatus.getText().toString() + "\n" +
                "✅ 数据开始接收\n" +
                "数据开始时间：" + dataStartTime);
    }

    // ==================== VideoRecorder.VideoListener 回调 ====================
    /**
     * 视频录制开始
     */
    @Override
    public void onVideoStarted(String videoPath, String startTime) {
        // 记录视频路径和开始时间戳
        this.videoFilePath = videoPath;
        detectionTimeStamp.setVideoStartTime(startTime);

        // 更新状态
        tvStatus.setText("✅ 视频录制已开始\n" +
                detectionTimeStamp.toString() + "\n" +
                "录制时长：90秒，请勿退出界面");

        // 禁用蓝牙检测按钮（避免中途切换设备）
        btnBluetoothDetect.setEnabled(false);
        btnBluetoothDetect.setAlpha(0.5f);

        // 开始接收蓝牙数据
        bluetoothService.startReceivingData();
    }

    /**
     * 视频录制完成
     */
    @Override
    public void onVideoFinished(String videoPath) {
        // 更新状态
        tvStatus.setText("✅ 视频录制完成\n" +
                "视频路径：" + videoPath + "\n" +
                "正在上传数据到后端...");

        // 获取蓝牙收集的数据
        OximeterData oximeterData = bluetoothService.getCollectedData();

// 先保存到本地（类似于上传逻辑）
        try {
            DataSaver.saveAllData(this, videoPath, oximeterData, detectionTimeStamp);
            tvStatus.setText(tvStatus.getText().toString() + "\n" +
                    "✅ 本地数据保存成功");
            Toast.makeText(this, "数据已保存到本地", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            tvStatus.setText(tvStatus.getText().toString() + "\n" +
                    "❌ 本地数据保存失败: " + e.getMessage());
            Toast.makeText(this, "本地保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        // 再上传数据（保持原逻辑）
        tvStatus.setText(tvStatus.getText().toString() + "\n" +
                "正在上传数据到后端...");
        uploadService.uploadAllData(oximeterData, videoPath, detectionTimeStamp, this);
    }

    /**
     * 视频录制错误
     */
    @Override
    public void onVideoError(String errorMsg) {
        // 更新状态（红色提示错误）
        tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        tvStatus.setText("❌ 视频录制错误\n" +
                "原因：" + errorMsg + "\n" +
                "请重新点击「开始检测」按钮");

        // 启用蓝牙检测按钮
        btnBluetoothDetect.setEnabled(true);
        btnBluetoothDetect.setAlpha(1.0f);

        // 隐藏视频预览
        previewView.setVisibility(View.GONE);
    }

    // ==================== DataUploadService.UploadListener 回调 ====================
    /**
     * 数据上传成功
     */
    @Override
    public void onUploadSuccess(String response) {
        // 更新状态（绿色提示成功）
        tvStatus.setTextColor(getResources().getColor(R.color.success_green));
        tvStatus.setText("✅ 所有数据上传成功\n" +
                detectionTimeStamp.toString() + "\n" +
                "视频路径：" + videoFilePath + "\n" +
                "服务器响应：" + response);

        // 重置按钮状态
        resetButtonState();

        // 隐藏视频预览
        previewView.setVisibility(View.GONE);
    }

    /**
     * 数据上传失败
     */
    @Override
    public void onUploadFailed(String errorMsg) {
        // 更新状态（红色提示失败）
        tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        tvStatus.setText("❌ 数据上传失败\n" +
                "原因：" + errorMsg + "\n" +
                "可重新点击「开始检测」按钮重试");

        // 重置按钮状态
        resetButtonState();

        // 隐藏视频预览
        previewView.setVisibility(View.GONE);
    }

    /**
     * 数据上传进度
     */
    @Override
    public void onUploadProgress(int progress) {
        // 更新上传进度
        tvStatus.setText("正在上传数据...\n" +
                "上传进度：" + progress + "%\n" +
                "视频路径：" + videoFilePath);
    }

    /**
     * 重置按钮状态
     */
    private void resetButtonState() {
        // 启用蓝牙检测按钮
        btnBluetoothDetect.setEnabled(true);
        btnBluetoothDetect.setAlpha(1.0f);

        // 保持开始检测按钮启用（可重新检测）
        btnStartDetection.setEnabled(true);
        btnStartDetection.setAlpha(1.0f);
    }

    /**
     * 页面销毁：释放资源
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 断开蓝牙连接
        if (bluetoothService != null) {
            bluetoothService.disconnect();
        }
        // 释放视频录制资源
        if (videoRecorder != null) {
            videoRecorder.releaseResources();
        }
    }

    /**
     * 页面暂停：停止预览
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (videoRecorder != null && !videoRecorder.isRecording()) {
            videoRecorder.releaseResources();
        }
    }

    /**
     * 页面恢复：重启预览（如果正在录制）
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (videoRecorder != null && videoRecorder.isRecording() && previewView.getVisibility() == View.VISIBLE) {
            videoRecorder.releaseResources(); // 重新初始化预览
        }
    }
}