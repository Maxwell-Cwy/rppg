package com.example.myapplication.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChartHelper {

    public static List<Double> mChartDataList = new ArrayList<>();
    private static Handler handler = new Handler(Looper.getMainLooper());
    private static boolean isAutoUpdating = false;
    private static Runnable autoUpdateRunnable;
    private static final long UPDATE_INTERVAL = 1500;
    private static TextView mTvHRV;

    public static void setLineChart(Context context, LineChart lineChart, List<Double> list) {
        lineChart.clear();
        mChartDataList.clear();
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setData(lineDataSet(context, list));
        lineChart.setDrawBorders(false);
        lineChart.animateY(1000);
        lineChart.setHighlightPerTapEnabled(false);
        lineChart.setTouchEnabled(false);
        lineChart.setScaleYEnabled(false);
        lineChart.setScaleXEnabled(true);
        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.getLegend().setEnabled(false);


        XAxis xAxis1 = lineChart.getXAxis();
        xAxis1.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis1.setAxisLineColor(Color.TRANSPARENT);
        xAxis1.setDrawAxisLine(false);
        xAxis1.setDrawGridLines(false);
        xAxis1.setDrawLabels(false);
        xAxis1.setEnabled(true);
        xAxis1.setAxisMinimum(0f);
        xAxis1.setAvoidFirstLastClipping(true);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
    }

    public static void addDataToChart(LineChart lineChart, double val) {
        if (null == mChartDataList) {
            Log.w("addDataToChart", "addDataToChart: " + "Data list is null.");
            return;
        }
        mChartDataList.add(val);

        Entry newEntry = getEntry(mChartDataList.size(), mChartDataList);

        LineData lineData = lineChart.getData();
        if (lineData != null) {
            LineDataSet dataSet = (LineDataSet) lineData.getDataSetByIndex(0);
            if (newEntry != null && dataSet != null) {
                Log.d("addDataToChart", "Adding entry: " + newEntry.toString());
                dataSet.addEntry(newEntry);
                lineData.notifyDataChanged();
                lineChart.notifyDataSetChanged();
                lineChart.setVisibleXRangeMaximum(4);
                lineChart.moveViewToX(dataSet.getEntryCount() - 1);
                lineChart.invalidate();
            } else {
                Log.e("addDataToChart", "Failed to add entry: newEntry is null or dataSet is null");
            }
        }
    }

    public static void startAutoUpdate(LineChart lineChart, TextView tvHRV) {
        mTvHRV = tvHRV;
        if (isAutoUpdating) {
            stopAutoUpdate();
        }
        isAutoUpdating = true;

        autoUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                double val = setVal();
                mTvHRV.setText((int) val + "");
                addDataToChart(lineChart, val);
                if (isAutoUpdating) {
                    handler.postDelayed(this, UPDATE_INTERVAL);
                }
            }
        };
        handler.post(autoUpdateRunnable);
    }

    // 停止自动添加数据
    public static void stopAutoUpdate() {
        isAutoUpdating = false;
        handler.removeCallbacks(autoUpdateRunnable);
    }

    public static double setVal() {
        Random rand = new Random();
        int min = 50;
        int max = 80;
        return rand.nextInt(max - min + 1) + min;
    }

    private static List<Entry> getDataSet(List<Double> dataList) {
        List<Entry> data = new ArrayList<>();
        int xVal = 1;
        for (int i = 0; i < dataList.size(); i++) {
            Double yVal = dataList.get(i);
            if (i != 0) {
                xVal = 1;
            }
            data.add(new Entry(Float.parseFloat(xVal + ""), Float.parseFloat(yVal + "")));

        }
        return data;
    }

    private static LineData lineDataSet(Context context, List<Double> dataList) {
        ArrayList<ILineDataSet> sets = new ArrayList<>();
        LineDataSet ds1 = new LineDataSet(getDataSet(dataList), "");
        ds1.setValueTextColor(0x000000);
        ds1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        ds1.setLineWidth(1f);
        ds1.setDrawCircles(false);
        ds1.setHighlightEnabled(false);
        ds1.setDrawValues(false);
        ds1.setDrawFilled(true);//填充色开关
        ds1.setFillDrawable(getDrawable(context, R.drawable.shape_blue_line_chart_fill));//设置填充渐变色
        ds1.setColor(context.getColor(R.color.app_blue));
        sets.add(ds1);
        return new LineData(sets);
    }

    private static Entry getEntry(int lastPosition, List<Double> list) {
        if (!list.isEmpty()) {
            int xVal = lastPosition + 1;
            return new Entry(Float.parseFloat(xVal + ""), Float.parseFloat(list.get(list.size() - 1) + ""));
        }
        return null;
    }

    private static Drawable getDrawable(Context context, @DrawableRes int id) {
        return ContextCompat.getDrawable(context, id);
    }
}
