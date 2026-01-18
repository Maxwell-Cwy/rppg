package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.myapplication.utils.DataSaver;
import com.google.android.material.button.MaterialButton;

public class StartActivity extends AppCompatActivity {

    private static final String TAG = "StartActivity";
    private static final int REQUEST_ALL_PERMISSIONS = 1001;
    private static final int REQUEST_SELECT_DEVICE = 1002;

    // UI
    private MaterialButton btnBluetoothDetect;
    private MaterialButton btnStartDetection;
    private MaterialButton btnManualUpload;
    private MaterialButton btnInputBloodPressure;

    // 服务
    private BluetoothService bluetoothService;

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
        setContentView(R.layout.activity_start);
        initViews();
        initServices();
        checkAllPermissions();
        bindEvents();
    }

    private void initViews() {
        btnBluetoothDetect = findViewById(R.id.btn_bluetooth_detect);
        btnStartDetection = findViewById(R.id.btn_start_detection);
        btnManualUpload = findViewById(R.id.btn_manual_upload);
        btnInputBloodPressure = findViewById(R.id.btn_input_blood_pressure);
        bluetoothService = BluetoothService.getInstance(this);
    }

    private void initServices() {
        bluetoothService.setBluetoothListener(new BluetoothService.BluetoothListener() {
            @Override
            public void onBluetoothConnected(String deviceName, String deviceAddress) {
                runOnUiThread(() -> {
                    btnStartDetection.setEnabled(true);
                    btnStartDetection.setAlpha(1.0f);
                    Toast.makeText(StartActivity.this, "蓝牙已连接：" + deviceName, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onBluetoothConnectFailed(String errorMsg) {
                runOnUiThread(() -> {
                    Toast.makeText(StartActivity.this, "连接失败：" + errorMsg, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onBluetoothDisconnected() {
                runOnUiThread(() -> {
                    Toast.makeText(StartActivity.this, "蓝牙断开", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onDataReceived(String hexData) {
                // StartActivity不需要处理数据，空实现
            }

            @Override
            public void onDataStartReceiving(String startTime) {
                // 空实现
            }

            @Override
            public void onDataStopReceiving(String endTime) {
                // 空实现
            }
        });
    }

    private void bindEvents() {
        btnInputBloodPressure.setOnClickListener(v -> showBloodPressureInputDialog());
        btnBluetoothDetect.setOnClickListener(v -> {
            startActivityForResult(new Intent(this, DeviceListActivity.class), REQUEST_SELECT_DEVICE);
        });

        btnStartDetection.setOnClickListener(v -> {
            if (!DataSaver.hasBloodPressure()) {
                Toast.makeText(this, "请先输入血压值", Toast.LENGTH_SHORT).show();
                return;
            }
            startDetection();
        });

        btnManualUpload.setOnClickListener(v -> {
            startActivity(new Intent(this, DataSelectionActivity.class));
        });
    }

    private void showBloodPressureInputDialog() {
        // 您的原代码，无变化
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText etSystolic = new EditText(this);
        etSystolic.setHint("收缩压 (如 120)");
        etSystolic.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etSystolic);

        final EditText etDiastolic = new EditText(this);
        etDiastolic.setHint("舒张压 (如 80)");
        etDiastolic.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etDiastolic);

        new AlertDialog.Builder(this)
                .setTitle("请输入血压值")
                .setView(layout)
                .setPositiveButton("确定", (dialog, which) -> {
                    String sysStr = etSystolic.getText().toString().trim();
                    String diaStr = etDiastolic.getText().toString().trim();

                    if (sysStr.isEmpty() || diaStr.isEmpty()) {
                        Toast.makeText(this, "请输入完整的血压值", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        int systolic = Integer.parseInt(sysStr);
                        int diastolic = Integer.parseInt(diaStr);

                        DataSaver.setBloodPressure(systolic, diastolic);

                        runOnUiThread(() -> {
                            Toast.makeText(this, "✅ 血压已录入：" + systolic + "/" + diastolic + " mmHg", Toast.LENGTH_LONG).show();
                        });
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void startDetection() {
        // 启动新CameraActivity进行检测
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
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
//        if (bluetoothService != null) bluetoothService.disconnect();
    }
}