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
        try {
            // ========== 基础信息（你原来就有的）==========
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"device_model\": \"").append(Build.MODEL).append("\",\n");
            json.append("  \"detect_start_time\": \"").append(data.getStartTime()).append("\",\n");
            json.append("  \"bluetooth_connect_time\": \"").append(safe(ts != null ? ts.getBluetoothConnectTime() : null)).append("\",\n");
            json.append("  \"data_start_time\": \"").append(safe(ts != null ? ts.getBluetoothDataStartTime() : null)).append("\",\n");
            json.append("  \"video_start_time\": \"").append(safe(ts != null ? ts.getVideoStartTime() : null)).append("\",\n");
            json.append("  \"total_packets\": ").append(data.getCount()).append(",\n");
            json.append("  \"valid_packets\": ").append(data.getValidCount()).append(",\n");

            // ========== 统计值（你原来就有的）==========
            json.append("  \"avg_spo2\": ").append(data.getAvgSpo2() >= 0 ? data.getAvgSpo2() : "null").append(",\n");
            json.append("  \"min_spo2\": ").append(data.getMinSpo2() >= 0 ? data.getMinSpo2() : "null").append(",\n");
            json.append("  \"max_spo2\": ").append(data.getMaxSpo2()).append(",\n");
            json.append("  \"avg_pr\": ").append(data.getAvgPr() >= 0 ? data.getAvgPr() : "null").append(",\n");
            json.append("  \"min_pr\": ").append(data.getMinPr() >= 0 ? data.getMinPr() : "null").append(",\n");
            json.append("  \"max_pr\": ").append(data.getMaxPr()).append(",\n");
            json.append("  \"temperature\": ").append(data.getTemperature() > 0 ? String.format("%.1f", data.getTemperature()) : "null").append(",\n");
            json.append("  \"pi\": ").append(data.getPi() >= 0 ? String.format("%.2f", data.getPi()) : "null").append(",\n");
            json.append("  \"respiration_rate\": ").append(data.getRespirationRate() > 0 ? data.getRespirationRate() : "null").append(",\n");
            json.append("  \"probe_status\": \"").append(data.getProbeStatus()).append("\",\n");
            json.append("  \"battery_level\": ").append(data.getBatteryLevel()).append(",\n");

            // ========== PPG 完整波形数据（新增，带采样率）==========
            json.append("  \"ppg_sample_rate_hz\": 5,\n");
            json.append("  \"ppg_data\": [\n");
            var ppgList = data.getPpgList();
            var barList = data.getBarList();
            int ppgSize = Math.min(ppgList.size(), barList.size());
            for (int i = 0; i < ppgSize; i++) {
                json.append("    {\"index\":").append(i)
                        .append(",\"wave\":").append(ppgList.get(i))
                        .append(",\"bar\":").append(barList.get(i)).append("}");
                if (i < ppgSize - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ],\n");

            // ========== HRV 数据（新增）==========
            json.append("  \"hrv_sample_rate\": \"1_pack_per_10_beats\",\n");
            json.append("  \"hrv_data\": [\n");
            var hrvList = data.getHrvList();
            for (int i = 0; i < hrvList.size(); i++) {
                json.append("    ").append(hrvList.get(i));
                if (i < hrvList.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ],\n");

            json.append("  \"raw_hex_data\": \"").append(data.toHexString().replace("\"", "\\\"")).append("\"\n");
            json.append("}");

            return json.toString();

        } catch (Exception e) {
            Log.e(TAG, "生成JSON失败", e);
            return "{\"error\": \"generate json failed\"}";
        }
    }
    private static String safe(String s) {
        return s != null ? s : "";
    }
}