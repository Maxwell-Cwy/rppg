package com.example.myapplication;

import static com.wuzhengai.examination.utils.AIConstants.PGG_REGULAR_LEN;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraFilter;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.model.DetectionTimeStamp;
import com.example.myapplication.model.OximeterData;
import com.example.myapplication.utils.ChartHelper;
import com.example.myapplication.utils.DataSaver;
import com.example.myapplication.utils.TimeUtils;
import com.example.myapplication.view.MyVIew;
import com.google.android.material.button.MaterialButton;
import com.github.mikephil.charting.charts.LineChart;
import com.wuzhengai.examination.auxiliaryTools.AuxiliaryCameraView;
import com.wuzhengai.examination.auxiliaryTools.AuxiliaryLisener;
import com.wuzhengai.examination.auxiliaryTools.MyCameraFilter;
import com.wuzhengai.examination.tfliterun.blazeface.Face;
import com.wuzhengai.examination.JSONBean.auto.PGGLSTMBean;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements VideoRecorder.VideoListener {

    private static final String TAG = "CameraActivity";

    // UI
    private MyAuxiliaryCameraView acv1;
    private MyVIew mMyView;
    private ProgressBar pb1;
    private TextView mTvHRV;
    private LineChart lineChart;
    private TextView mTvCurStatus;
    private TextView tvCountdown;
    private MaterialButton btnExitPreview;

    // 服务
    private VideoRecorder videoRecorder;
    private BluetoothService bluetoothService; // 假设从Application获取或新初始化

    // 数据
    private DetectionTimeStamp timeStamp;
    private OximeterData oximeterData;
    private String videoFilePath;

    private CountDownTimer countDownTimer;

    private boolean isDetectionInProgress = false;
    private boolean shouldSaveAndUpload = false;

    // SDK相关
    private String id = "0";
    private String asStr = "";
    private Handler handler;
    private final int CODE_FACE = 1;
    private final int CODE_PROGRESS = 2;
    private final int CODE_MESSAGE = 3;
    private final int CODE_FAILE = 4;
    public final String CAMERA_ID = "1";
    private boolean checkFace = false;
    private String age = "";
    private String gender = "";
    private int mProgressMax = PGG_REGULAR_LEN;
//    private int mProgressMax = 2200;
    private int mCurrentProgress = 0;
    private boolean isRecording = false;
    private PGGLSTMBean pgglstmBean;
    private boolean isVideoFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        initViews();
        initServices();
        startDetection(); // 自动开始检测
    }

    private void initViews() {
        acv1 = findViewById(R.id.acv1);
        mMyView = findViewById(R.id.myview);
        pb1 = findViewById(R.id.pb1);
        mTvHRV = findViewById(R.id.tv_vale);
        lineChart = findViewById(R.id.lineChart);
        mTvCurStatus = findViewById(R.id.tv_cur_status);
        tvCountdown = findViewById(R.id.tv_countdown);
        btnExitPreview = findViewById(R.id.btn_exit_preview);
        bluetoothService = BluetoothService.getInstance(this);
        btnExitPreview.setOnClickListener(v -> stopDetectionEarly());
        shouldSaveAndUpload = false;
        isVideoFinished = false;

        // 初始化Handler
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case CODE_FACE:
                        Face face = (Face) msg.obj;
                        if (!isRecording) {
                            isRecording = true;
                            ChartHelper.startAutoUpdate(lineChart, mTvHRV); // 开始自动更新折线图和心率显示，假设ChartHelper存在
                        }
                        if (face == null) {
                            mMyView.drawMy(null);
                            break;
                        }
                        if (mCurrentProgress >= mProgressMax) {
                            mMyView.drawMy(null);
                            break;
                        }
                        int scale = 1;
                        int sx = (int) (face.left * scale);
                        int sy = (int) (face.top * scale);
                        int ex = (int) (face.width * scale) - sx;
                        int ey = (int) (face.height * scale) - sy;
                        mMyView.drawMy(face, sx, sy, ex, ey);// 绘制人脸框
                        break;
                    case CODE_PROGRESS:
                        mProgressMax = msg.arg1;
                        mCurrentProgress = msg.arg2;
                        pb1.setMax(mProgressMax + 10);
                        pb1.setProgress(mCurrentProgress);
                        if (mProgressMax == mCurrentProgress) {
                            mMyView.drawMy(null);// 进度达到最大值时清除绘制
                        }
                        break;
                    case CODE_MESSAGE:
                        pgglstmBean = (PGGLSTMBean) msg.obj;
                        // 这里不进行视频合成和上传，直接处理结果，例如更新UI或保存数据
                        setTvCurStatus("分析完成");
                        // 可以在这里调用stopDetection() 如果需要基于进度结束
                        if (isVideoFinished) {
                            Log.d(TAG, "两者完成：SDK 在视频后");
                            runOnUiThread(() -> stopDetection());
                        } else {
                            Log.d(TAG, "SDK 完成，等待视频");
                        }
                        break;
                    case CODE_FAILE:
                        Toast.makeText(CameraActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        pb1.setMax(PGG_REGULAR_LEN); // 假设常量已定义
        ChartHelper.setLineChart(this, lineChart, new ArrayList<>()); // 初始化图表，假设ChartHelper存在
    }

    private void initServices() {
        // 假设BluetoothService从主Activity共享，或重新连接
        timeStamp = new DetectionTimeStamp();
        oximeterData = new OximeterData();
        bluetoothService.setBluetoothListener(new BluetoothService.BluetoothListener() {
            @Override
            public void onBluetoothConnected(String deviceName, String deviceAddress) {
                runOnUiThread(() -> {
                    Toast.makeText(CameraActivity.this, "复用蓝牙连接：" + deviceName, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onBluetoothConnectFailed(String errorMsg) {
                runOnUiThread(() -> {
                    Toast.makeText(CameraActivity.this, "连接失败：" + errorMsg, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onBluetoothDisconnected() {
                runOnUiThread(() -> {
                    Toast.makeText(CameraActivity.this, "蓝牙断开", Toast.LENGTH_SHORT).show();
                    stopDetectionEarly();
                });
            }

            @Override
            public void onDataReceived(String hexData) {
                oximeterData.addData(hexData);
            }

            @Override
            public void onDataStartReceiving(String startTime) {
                timeStamp.setBluetoothDataStartTime(startTime);
            }

            @Override
            public void onDataStopReceiving(String endTime) {
                timeStamp.setBluetoothDataEndTime(endTime);
            }

        });
    }

    private void startDetection() {
        oximeterData.clear();
        timeStamp.clear();

        isDetectionInProgress = true;
        shouldSaveAndUpload = true;

        // 启动SDK相机
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            // 请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 11);
        }
    }

    private void startCamera() {
        // 设置前置摄像头过滤器
        acv1.setFilter(new FrontCameraFilter());
        acv1.setGrayMode(false); // 根据需要设置
        acv1.setMirroMode(true); // 设置镜像模式
        acv1.initCamera(this); // 初始化摄像头
        acv1.getAuxiliaryCore().setOnlyFaceMode(false); // 设置模式
        acv1.getAuxiliaryCore().setEmotionSplit(45);

        // 配置参数，如年龄性别等（从Intent或默认）
        getBundleData(); // 添加从参考
        setCameraBundle(true);

        acv1.getAuxiliaryCore().setSbpProgressLisener(new AuxiliaryLisener() {
            @Override
            public void updateProgress(int max, int now) {
                Log.i(TAG, "updateProgress: max = " + max + "; now = " + now);
                Message message = handler.obtainMessage();
                message.arg1 = max;
                message.arg2 = now;
                message.what = CODE_PROGRESS;
                handler.sendMessage(message); // 发送进度更新消息
            }

            @Override
            public void detectFace(Face face) {
                Log.i(TAG, "detectFace: face = " + (face != null ? face.toString() : "null"));
                Message message = handler.obtainMessage();
                message.what = CODE_FACE;
                message.obj = face;
                handler.sendMessage(message);// 发送人脸检测消息
            }

            @Override
            public void finalMessage(String result) {
                Log.i(TAG, "finalMessage (SDK结果已出): " + result);
                Gson gson = new Gson();
                pgglstmBean = gson.fromJson(result, PGGLSTMBean.class);

                // 发送消息更新UI
                Message message = handler.obtainMessage(CODE_MESSAGE, pgglstmBean);
                handler.sendMessage(message);


//                Log.i(TAG, "finalMessage: " + result);
//                acv1.getAuxiliaryCore().setUnderEnd(true);
//                acv1.getAuxiliaryCore().readyEnd();
//                Gson gson = new Gson();
//                pgglstmBean = gson.fromJson(result, PGGLSTMBean.class);// 解析结果
//                Message message = handler.obtainMessage();
//                message.what = CODE_MESSAGE;
//                message.obj = pgglstmBean;
//                handler.sendMessage(message);
                // 可以在这里停止录制如果进度满
            }


            public boolean onWorkBitmap(Bitmap bitmap) {
                // 不需要收集bitmaps，因为不合成视频
                return super.onWorkBitmap(bitmap);
            }

            @Override
            public void onFail(int failcode, String message, Exception e) {
                Log.e(TAG, "onFail: code = " + failcode + " message = " + message);
                e.printStackTrace();
                Message errMessage = handler.obtainMessage();
                errMessage.what = CODE_FAILE;
                errMessage.obj = message;
                handler.sendMessage(errMessage);
            }

            @Override
            public void updateEmotion(String emotion) {
                Log.i(TAG, "updateEmotion: " + emotion);
                Log.w("emotion",acv1.getAuxiliaryCore().getEmotionStr());
            }
        });
        // 等待 cameraProvider 初始化完成后再启动录制
        Handler handler = new Handler(Looper.getMainLooper());
        final int maxAttempts = 20;  // 最多尝试2秒 (20 * 100ms)
        final Runnable checkRunnable = new Runnable() {
            int attempts = 0;

            @Override
            public void run() {
                ProcessCameraProvider provider = acv1.getProvider();
                if (provider != null) {
                    Log.d(TAG, "CameraProvider initialized, starting recording");
                    videoRecorder = new VideoRecorder(CameraActivity.this, CameraActivity.this, provider, CameraActivity.this, CameraSelector.DEFAULT_FRONT_CAMERA);
                    videoRecorder.startRecording(90_000);
                } else if (attempts < maxAttempts) {
                    attempts++;
                    Log.w(TAG, "CameraProvider not ready yet, retrying... Attempt: " + attempts);
                    handler.postDelayed(this, 100);  // 每100ms检查一次
                } else {
                    Log.e(TAG, "Failed to initialize CameraProvider after " + maxAttempts + " attempts");
                    Toast.makeText(CameraActivity.this, "摄像头初始化失败，请重试", Toast.LENGTH_SHORT).show();
                    // 可选：停止检测
                    stopDetectionEarly();
                }
            }
        };
        handler.post(checkRunnable);  // 启动检查
//
//        // 初始化VideoRecorder，使用acv1的 public getProvider()
//        videoRecorder = new VideoRecorder(this, this, acv1.getProvider(), this, CameraSelector.DEFAULT_FRONT_CAMERA);
//
//        // 启动视频录制
//        videoRecorder.startRecording(90_000);
    }

    private void getBundleData() {
        checkFace = getIntent().getBooleanExtra("checkFace", true);
        age = getIntent().getStringExtra("age");
        gender = getIntent().getStringExtra("gender");
    }

    private void setCameraBundle(boolean isConfig) {
        if (acv1 == null) {
            return;
        }
        if (!isConfig) {
            return;
        }
        acv1.getAuxiliaryCore().setCheckFace(checkFace);
        acv1.getAuxiliaryCore().setAge(age);
        acv1.getAuxiliaryCore().setGender(gender);
    }

    private void setTvCurStatus(String statusStr) {
        mTvCurStatus.setText(statusStr);
    }

    private void stopDetectionEarly() {
        isDetectionInProgress = false;
        shouldSaveAndUpload = false;

        oximeterData.clear();
        timeStamp.clear();

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        tvCountdown.setVisibility(View.GONE);

        if (videoRecorder != null) {
            videoRecorder.stopRecording();
            videoRecorder.releaseResources();
        }

        if (bluetoothService != null) {
            bluetoothService.stopReceivingData();
        }

        videoFilePath = null;

        Toast.makeText(this, "检测提前终止", Toast.LENGTH_LONG).show();
        DataSaver.setBloodPressure(-1, -1);
        finish(); // 返回主Activity
    }

    private void stopDetection() {
        isDetectionInProgress = false;
        timeStamp.setVideoEndTime(TimeUtils.getPreciseTimeStamp());
        bluetoothService.stopReceivingData();

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        tvCountdown.setVisibility(View.GONE);

        if (shouldSaveAndUpload) {
            String report = "检测已完成！\n检测报告：\n" + oximeterData.generateReport();
            try {
                // 将 pgglstmBean 转为 JSON 字符串
                Gson gson = new Gson();
                String pgglstmJson = gson.toJson(pgglstmBean);
                DataSaver.saveAllData(this, videoFilePath, oximeterData, timeStamp);
                Toast.makeText(this, "本地保存成功", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, ResultActivity.class);
                intent.putExtra("REPORT", report);
                intent.putExtra("VIDEO_PATH", videoFilePath);
                intent.putExtra("PGG_LSTM_JSON", pgglstmJson);
                Log.d(TAG, "准备跳转 ResultActivity...");
                startActivity(intent);
//                finish();
            } catch (Exception e) {
                Toast.makeText(this, "本地保存失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "检测未完成，数据未保存", Toast.LENGTH_SHORT).show();
        }
        DataSaver.setBloodPressure(-1, -1);
        ChartHelper.stopAutoUpdate(); // 停止图表更新
//        finish(); // 结束，返回主Activity
    }

    @Override
    public void onVideoStarted(String videoPath, String startTime) {
        videoFilePath = videoPath;
        timeStamp.setVideoStartTime(startTime);
        setTvCurStatus("正在扫描中...");
        // 视频录制开始后，启动倒计时
        tvCountdown.setText("90");
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new CountDownTimer(90_000, 1_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvCountdown.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("0");
                if (videoRecorder != null) {
                    // 倒计时结束，触发视频停止 -> 进而触发 onVideoFinished -> stopDetection
                    videoRecorder.stopRecording();
                }
            }
        }.start();

        // 视频录制开始后1秒内启动蓝牙采集（这里延迟500ms）
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            bluetoothService.startReceivingData();
        }, 500);
    }

    @Override
    public void onVideoFinished(String videoPath, String endTime) {
        this.videoFilePath = videoPath;
        timeStamp.setVideoEndTime(endTime);
        isVideoFinished = true; // 标记视频已完成

        setTvCurStatus("视频录制完成，检查分析结果...");

        if (pgglstmBean != null) {
            // 如果此时 SDK 已经分析完了，直接跳转
            Log.d(TAG, "两者完成：视频在 SDK 后");
            stopDetection();
        } else {
            // 如果 SDK 还没出结果，显示等待提示，不执行 stopDetection
            setTvCurStatus("视频已好，正在等待分析报告...");
            Log.d(TAG, "Video finished but SDK still processing...");
        }
    }

    @Override
    public void onVideoError(String errorMsg) {
        Log.e(TAG, "Video recording error: " + errorMsg);
        DataSaver.setBloodPressure(-1, -1);
        new AlertDialog.Builder(this)
                .setTitle("视频录制失败")
                .setMessage(errorMsg)
                .setPositiveButton("确定", (d, w) -> {
                    d.dismiss();
                    stopDetectionEarly();  // 错误后自动停止
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (bluetoothService != null) bluetoothService.disconnect(); //要主动断开吗？
        if (videoRecorder != null) videoRecorder.releaseResources();
        if (acv1 != null && acv1.getAuxiliaryCore() != null) {
            acv1.getAuxiliaryCore().onDestroy();
        }
        isRecording = false;
    }

    // 自定义前置摄像头过滤器
    private static class FrontCameraFilter implements CameraFilter {
        @NonNull
        @Override
        public List<CameraInfo> filter(@NonNull List<CameraInfo> cameraInfos) {
            List<CameraInfo> result = new ArrayList<>();
            for (CameraInfo info : cameraInfos) {
                if (info.getLensFacing() == CameraSelector.LENS_FACING_FRONT) {
                    result.add(info);
                }
            }
            return result.isEmpty() ? cameraInfos : result;
        }
    }
}