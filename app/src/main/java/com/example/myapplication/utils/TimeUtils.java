package com.example.myapplication.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 时间工具类：提供各种格式的时间戳
 */
public class TimeUtils {

    /**
     * 获取精确到毫秒的时间戳（用于日志、时间戳对齐）
     * 格式：2025-04-05 15:22:33.456
     */
    public static String getPreciseTimeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     /**
     * 获取用于文件名的超精准时间戳（毫秒级，永不重复）
     * 格式：20250405152233456
     */
    public static String getFileNameTimeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat(
                "yyyyMMddHHmmssSSS", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * 获取简洁时间戳（用于文件夹名，用户一眼就能看懂）
     * 格式：20250405_152233
     */
    public static String getSimpleTimeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * 获取中文格式时间（用于报告标题）
     * 格式：2025年04月05日 15:22:33
     */
    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat(
                "yyyy年MM月dd日 HH:mm:ss", Locale.CHINA);
        return sdf.format(new Date());
    }
}