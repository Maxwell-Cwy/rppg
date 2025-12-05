package com.example.myapplication.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 血氧仪数据模型：存储所有检测数据
 */
public class OximeterData {
    // 数据列表（存储每一条蓝牙数据）
    private List<String> dataList = new ArrayList<>();
    // 血氧值（最终计算结果）
    private int spo2Value;
    // 脉搏率（最终计算结果）
    private int pulseRate;

    // 添加单条数据
    public void addData(String data) {
        dataList.add(data);
    }

    // Getter & Setter
    public List<String> getDataList() {
        return dataList;
    }

    public int getSpo2Value() {
        return spo2Value;
    }

    public void setSpo2Value(int spo2Value) {
        this.spo2Value = spo2Value;
    }

    public int getPulseRate() {
        return pulseRate;
    }

    public void setPulseRate(int pulseRate) {
        this.pulseRate = pulseRate;
    }

    // 判断是否有数据
    public boolean hasData() {
        return !dataList.isEmpty();
    }
}