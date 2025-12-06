package com.example.myapplication.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 指夹式血氧仪数据解析模型
 * 完全按照《手指血氧协议20230117 英文版》实现
 * 兼容 Java 11，已添加所有必要 getter
 */
public class OximeterData {

    private static final String TAG = "OximeterData";

    // 原始数据
    private final List<String> rawDataList = new ArrayList<>();

    // 实时值
    private int spo2 = -1;
    private int pr = -1;
    private double temperature = -1.0;
    private double pi = -1.0;
    private int respirationRate = -1;
    private String probeStatus = "未知";
    private int batteryLevel = -1;

    // 统计值
    private int validCount = 0;

    private int sumSpo2 = 0, sumPr = 0;
    private int minSpo2 = 999, maxSpo2 = 0;
    private int minPr = 999, maxPr = 0;

    private String startTime;

    public void addData(String hexData) {
        if (rawDataList.isEmpty()) {
            startTime = com.example.myapplication.utils.TimeUtils.getPreciseTimeStamp();
        }
        rawDataList.add(hexData);
        parsePacket(hexData);
    }

    private void parsePacket(String hexData) {
        String clean = hexData.replaceAll("\\s", "").toUpperCase();
        if (clean.length() < 12 || !clean.startsWith("FFFE")) {
            return;
        }

        try {
            int ll = Integer.parseInt(clean.substring(4, 6), 16);
            int csGiven = Integer.parseInt(clean.substring(6, 8), 16);
            String deviceId = clean.substring(8, 10);
            String cmdId = clean.substring(10, 12);

            if (!"23".equals(deviceId)) {
                return;
            }

            // 校验和计算：sum(LL + deviceID + CMD + DATA) & 0xFF
            int calcCs = ll + 0x23 + Integer.parseInt(cmdId, 16);
            for (int i = 12; i < clean.length(); i += 2) {
                calcCs += Integer.parseInt(clean.substring(i, i + 2), 16);
            }
            calcCs &= 0xFF;

            if (calcCs != csGiven) {
                Log.w(TAG, "校验和错误: 期望=" + String.format("%02X", csGiven) +
                        " 计算=" + String.format("%02X", calcCs) + " 数据=" + hexData);
                return;
            }

            String dataHex = clean.substring(12);
            byte[] data = hexStringToByteArray(dataHex);

            if ("95".equals(cmdId) && data.length >= 7) {  // 改为 >=7，支持设备实际发送的7字节数据
                parseCmd95(data);
            } else if ("99".equals(cmdId) && data.length >= 1) {
                parseCmd99(data);
            }

        } catch (Exception e) {
            Log.e(TAG, "解析异常: " + hexData, e);
        }
    }

    private void parseCmd95(byte[] d) {
        int b0 = d[0] & 0xFF;
        int b1 = d[1] & 0xFF;
        int b2 = d[2] & 0xFF;
        int b3 = d[3] & 0xFF;
        int b4 = d[4] & 0xFF;
        int b5 = d[5] & 0xFF;
        int b6 = d[6] & 0xFF;
        int b7 = (d.length >= 8) ? d[7] & 0xFF : -1;  // 如果有第8字节，才解析呼吸率，否则-1

        // 探头状态
        int probeCode = (b0 >> 2) & 0x07;
        switch (probeCode) {
            case 0: probeStatus = "正常"; break;
            case 1: probeStatus = "探头未接"; break;
            case 2: probeStatus = "电流过大"; break;
            case 3: probeStatus = "探头故障"; break;
            case 4: probeStatus = "手指脱落"; break;
            default: probeStatus = "未知状态(" + probeCode + ")"; break;
        }

        // PR
        pr = b1 + ((b2 >> 7) & 1) * 256;
        if (pr < 25 || pr > 300) pr = -1;

        // SpO2
        spo2 = b2 & 0x7F;
        if (spo2 > 100) spo2 = -1;

        // 体温
        if (b3 >= 1 && b3 <= 99 && b4 <= 9) {
            temperature = b3 + b4 / 10.0;
        } else {
            temperature = -1.0;
        }

        // PI
        if (b5 != 0x7F && b6 != 0x7F && b5 <= 20) {
            pi = b5 + b6 / 100.0;
        } else {
            pi = -1.0;
        }

        // 呼吸率（仅如果存在第8字节）
        if (b7 >= 4 && b7 <= 120) {
            respirationRate = b7;
        } else {
            respirationRate = -1;
        }

        // 统计
        if (spo2 > 0 || pr > 0) {
            validCount++;
            if (spo2 > 0 && spo2 <= 100) {
                sumSpo2 += spo2;
                minSpo2 = Math.min(minSpo2, spo2);
                maxSpo2 = Math.max(maxSpo2, spo2);
            }
            if (pr >= 25 && pr <= 300) {
                sumPr += pr;
                minPr = Math.min(minPr, pr);
                maxPr = Math.max(maxPr, pr);
            }
        }
    }

    private void parseCmd99(byte[] d) {
        batteryLevel = d[0] & 0x03;
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    | Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    // ====================== 对外接口 ======================
    public String toHexString() {
        return String.join(",", rawDataList);
    }

    public int getCount() { return rawDataList.size(); }

    public String getStartTime() { return startTime != null ? startTime : ""; }

    public boolean hasData() { return !rawDataList.isEmpty(); }

    public int getValidCount() { return validCount; }  // 关键！DataSaver 需要的

    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("指夹式血氧检测报告\n");
        sb.append("══════════════════════════\n");
        sb.append("检测时间：").append(getStartTime()).append("\n");
        sb.append("数据包总数：").append(rawDataList.size()).append(" 条\n");
        sb.append("有效数据：").append(validCount).append(" 条\n");
        sb.append("探头状态：").append(probeStatus).append("\n\n");

        if (spo2 >= 0) {
            sb.append(String.format("当前血氧 SpO₂： %3d%%\n", spo2));
            if (validCount > 0) {
                sb.append(String.format("  ├ 平均值域：%d ~ %d %%\n", minSpo2 == 999 ? 0 : minSpo2, maxSpo2));
                sb.append(String.format("  └ 平均：%.1f %%\n", sumSpo2 * 1.0 / validCount));
            }
        }
        if (pr >= 0) {
            sb.append(String.format("当前心率 PR：   %3d bpm\n", pr));
            if (validCount > 0) {
                sb.append(String.format("  ├ 平均：%d bpm\n", sumPr / validCount));
            }
        }
        if (temperature > 0) sb.append(String.format("体温：         %.1f ℃\n", temperature));
        if (pi >= 0) sb.append(String.format("灌注指数 PI：  %.2f%%\n", pi));
        if (respirationRate > 0) sb.append(String.format("呼吸率：       %d 次/分\n", respirationRate));

        if (batteryLevel >= 0) {
            String[] bats = {"电量空", "电量低", "电量中等", "电量充足"};
            sb.append("设备电量：     ").append(bats[batteryLevel]).append("\n");
        }

        sb.append("══════════════════════════\n");
        sb.append("正常参考：SpO₂≥95% | PR 60-100 | 体温36.0-37.2℃\n");
        return sb.toString();
    }

    // Getter
    public int getSpo2() { return spo2; }
    public int getPr() { return pr; }
    public double getTemperature() { return temperature; }
    public double getPi() { return pi; }
    public int getRespirationRate() { return respirationRate; }
    public String getProbeStatus() { return probeStatus; }
    public int getBatteryLevel() { return batteryLevel; }

    public int getAvgSpo2() { return validCount > 0 ? sumSpo2 / validCount : -1; }
    public int getMinSpo2() { return minSpo2 == 999 ? -1 : minSpo2; }
    public int getMaxSpo2() { return maxSpo2; }
    public int getAvgPr() { return validCount > 0 ? sumPr / validCount : -1; }
    public int getMinPr() { return minPr == 999 ? -1 : minPr; }
    public int getMaxPr() { return maxPr; }

    public void clear() {
        rawDataList.clear();
        startTime = null;
        spo2 = pr = -1;
        temperature = pi = -1.0;
        respirationRate = batteryLevel = -1;
        probeStatus = "未知";
        validCount = sumSpo2 = sumPr = 0;
        minSpo2 = minPr = 999;
        maxSpo2 = maxPr = 0;
    }
}