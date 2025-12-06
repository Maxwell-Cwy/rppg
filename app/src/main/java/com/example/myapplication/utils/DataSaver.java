package com.example.myapplication.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.example.myapplication.model.DetectionTimeStamp;
import com.example.myapplication.model.OximeterData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class DataSaver {
    private static final String TAG = "DataSaver";

    public static void saveAllData(Context context,
                                   String videoPath,
                                   OximeterData oximeterData,
                                   DetectionTimeStamp timeStamp) {
        try {
            File rootDir = new File(context.getExternalFilesDir(null), "OximeterRecords");
            if (!rootDir.exists()) rootDir.mkdirs();

            String timeFolderName = TimeUtils.getSimpleTimeStamp(); // 2025-04-05_15-32-28
            File timeDir = new File(rootDir, timeFolderName);
            timeDir.mkdirs();

            // 1. 复制视频
            File sourceVideo = new File(videoPath);
            File targetVideo = new File(timeDir, "检测视频_90秒.mp4");
            if (sourceVideo.exists()) {
                Files.copy(sourceVideo.toPath(), targetVideo.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Log.i(TAG, "视频保存成功: " + targetVideo.getAbsolutePath());
            }

            // 2. 保存原始数据
            File rawFile = new File(timeDir, "01_原始数据.txt");
            Files.write(rawFile.toPath(), oximeterData.toHexString().getBytes());

            // 3. 保存报告
            File reportFile = new File(timeDir, "02_检测报告.txt");
            Files.write(reportFile.toPath(), oximeterData.generateReport().getBytes("UTF-8"));

            // 4. 保存 JSON
            File jsonFile = new File(timeDir, "检测信息.json");
            String json = generateJson(oximeterData, timeStamp);
            Files.write(jsonFile.toPath(), json.getBytes("UTF-8"));

            Log.e(TAG, "检测数据已完整保存！\n路径: " + timeDir.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "本地保存失败", e);
            throw new RuntimeException("保存失败: " + e.getMessage(), e);
        }
    }

    private static String generateJson(OximeterData data, DetectionTimeStamp ts) {
        return "{\n" +
                "  \"device_model\": \"" + Build.MODEL + "\",\n" +
                "  \"detect_start_time\": \"" + data.getStartTime() + "\",\n" +
                "  \"bluetooth_connect_time\": \"" + safe(ts != null ? ts.getBluetoothConnectTime() : null) + "\",\n" +
                "  \"data_start_time\": \"" + safe(ts != null ? ts.getBluetoothDataStartTime() : null) + "\",\n" +
                "  \"video_start_time\": \"" + safe(ts != null ? ts.getVideoStartTime() : null) + "\",\n" +
                "  \"total_packets\": " + data.getCount() + ",\n" +
                "  \"valid_packets\": " + data.getValidCount() + ",\n" +
                "  \"avg_spo2\": " + (data.getAvgSpo2() >= 0 ? data.getAvgSpo2() : "null") + ",\n" +
                "  \"min_spo2\": " + (data.getMinSpo2() >= 0 ? data.getMinSpo2() : "null") + ",\n" +
                "  \"max_spo2\": " + data.getMaxSpo2() + ",\n" +
                "  \"avg_pr\": " + (data.getAvgPr() >= 0 ? data.getAvgPr() : "null") + ",\n" +
                "  \"min_pr\": " + (data.getMinPr() >= 0 ? data.getMinPr() : "null") + ",\n" +
                "  \"max_pr\": " + data.getMaxPr() + ",\n" +
                "  \"temperature\": " + (data.getTemperature() > 0 ? String.format("%.1f", data.getTemperature()) : "null") + ",\n" +
                "  \"pi\": " + (data.getPi() >= 0 ? String.format("%.2f", data.getPi()) : "null") + ",\n" +
                "  \"respiration_rate\": " + (data.getRespirationRate() > 0 ? data.getRespirationRate() : "null") + ",\n" +
                "  \"probe_status\": \"" + data.getProbeStatus() + "\",\n" +
                "  \"battery_level\": " + data.getBatteryLevel() + "\n" +
                "}";
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }
}