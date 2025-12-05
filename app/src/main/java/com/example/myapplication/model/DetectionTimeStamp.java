package com.example.myapplication.model;

/**
 * 时间戳模型：记录所有关键时间点
 */
public class DetectionTimeStamp {
    // 蓝牙连接成功时间戳
    private String bluetoothConnectTime;
    // 视频录制开始时间戳
    private String videoStartTime;
    // 蓝牙数据开始检测时间戳
    private String bluetoothDataStartTime;

    // 空构造
    public DetectionTimeStamp() {}

    // Getter & Setter
    public String getBluetoothConnectTime() {
        return bluetoothConnectTime;
    }

    public void setBluetoothConnectTime(String bluetoothConnectTime) {
        this.bluetoothConnectTime = bluetoothConnectTime;
    }

    public String getVideoStartTime() {
        return videoStartTime;
    }

    public void setVideoStartTime(String videoStartTime) {
        this.videoStartTime = videoStartTime;
    }

    public String getBluetoothDataStartTime() {
        return bluetoothDataStartTime;
    }

    public void setBluetoothDataStartTime(String bluetoothDataStartTime) {
        this.bluetoothDataStartTime = bluetoothDataStartTime;
    }

    // 格式化显示所有时间戳
    @Override
    public String toString() {
        return "蓝牙连接成功: " + bluetoothConnectTime + "\n" +
                "视频开始录制: " + videoStartTime + "\n" +
                "数据开始检测: " + bluetoothDataStartTime;
    }
}