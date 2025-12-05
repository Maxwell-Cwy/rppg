package com.example.myapplication.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 时间工具类：生成标准时间戳
 */
public class TimeUtils {
    /**
     * 获取精确到毫秒的时间戳（格式：yyyy-MM-dd HH:mm:ss.SSS）
     */
    public static String getPreciseTimeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS",
                Locale.getDefault()
        );
        return sdf.format(new Date());
    }

    /**
     * 获取用于文件名的时间戳（格式：yyyyMMddHHmmssSSS）
     */
    public static String getFileNameTimeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat(
                "yyyyMMddHHmmssSSS",
                Locale.getDefault()
        );
        return sdf.format(new Date());
    }
}