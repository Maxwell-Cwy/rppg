package com.example.myapplication.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.wuzhengai.examination.tfliterun.blazeface.Face;
import com.wuzhengai.examination.tfliterun.pfld.FaceLandMarker;

import java.util.List;

public class MyVIew extends View {
    Face d = null;

    FaceLandMarker flm = null;

    //    起始xy
    int sx = 0;
    int sy = 0;

    int ex = 0;
    int ey = 0;
    private float[] nroi;
    private float[] froi;
    int viewWidth = 1;
    int viewHeight = 1;

    public MyVIew(Context context) {
        super(context);
    }

    public MyVIew(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MyVIew(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint p = new Paint();
        p.setColor(0xFF0E7DEC);
        p.setStrokeWidth(4);
        p.setStyle(Paint.Style.STROKE);
        if (d != null) {
            // 绘制面部检测框为正方形
            drawSquareFaceRect(canvas, d, p);

//            float t = Math.max(d.top - (d.height / 7), 0);
//            float l = Math.max(d.left - (d.width / 10), 0);
//            float w = Math.min(d.width + (d.width / 10), 1);
//            canvas.drawRect(d.left * viewWidth, t * viewHeight, (d.left + d.width) * viewWidth, (t + d.height) * viewHeight, p);
            drawOneRect(canvas, (int) (d.snDetections.getEye_r()[1] * viewHeight), (int) (d.snDetections.getEye_r()[0] * viewWidth),p);
            drawOneRect(canvas, (int) (d.snDetections.getEye_l()[1] * viewHeight), (int) (d.snDetections.getEye_l()[0] * viewWidth), p);
            drawOneRect(canvas, (int) (d.snDetections.getNose()[1] * viewHeight), (int) (d.snDetections.getNose()[0] * viewWidth), p);
            drawOneRect(canvas, (int) (d.snDetections.getMouth()[1] * viewHeight), (int) (d.snDetections.getMouth()[0] * viewWidth), p);
            drawOneRect(canvas, (int) (d.snDetections.getEar_r()[1] * viewHeight), (int) (d.snDetections.getEar_r()[0] * viewWidth), p);
            drawOneRect(canvas, (int) (d.snDetections.getEar_l()[1] * viewHeight), (int) (d.snDetections.getEar_l()[0] * viewWidth), p);

        }

        if (flm != null) {
            List<FaceLandMarker.FaceKeyPoint> keyPoints = flm.getKeyPoints();
            for (int i = 0; i < keyPoints.size(); i++) {
                boolean draw = true;
                if (draw) {
                    FaceLandMarker.FaceKeyPoint point = keyPoints.get(i);
                    int x = (int) (point.x * (ex));
                    int y = (int) (point.y * (ey));

                    canvas.drawRect(x - 2 + sx, y - 2 + sy, x + 2 + sx, y + 2 + sy, p);

                }
            }
        }

        p.setStrokeWidth(15);
        p.setStyle(Paint.Style.STROKE);
        if (froi != null && froi.length > 0) {
            drawSquareROI(canvas, froi, p);
//            canvas.drawRect(froi[0] * viewWidth, froi[1] * viewHeight, froi[2] * viewWidth, froi[3] * viewHeight, p);
        }
        if (nroi != null && nroi.length > 0) {
            drawSquareROI(canvas, nroi, p);
//            canvas.drawRect(nroi[0] * viewWidth, nroi[1] * viewHeight, nroi[2] * viewWidth, nroi[3] * viewHeight, p);
        }
    }

    // 绘制正方形的面部检测框
    private void drawSquareFaceRect(Canvas canvas, Face face, Paint paint) {
        // 原始位置计算
        float t = Math.max(face.top - (face.height / 7), 0);
        float l = Math.max(face.left - (face.width / 10), 0);
        float adjustedWidth = Math.min(face.width + (face.width / 5), 1);

        // 计算中心点
        float centerX = (face.left + face.width / 2) * viewWidth;
        float centerY = (t + face.height / 2) * viewHeight;

        // 根据宽度和高度的最大值确定正方形的边长
        float size = Math.max(face.width * viewWidth, face.height * viewHeight);

        // 绘制正方形
        canvas.drawRect(
                centerX - size / 2,
                centerY - size / 2,
                centerX + size / 2,
                centerY + size / 2,
                paint
        );
    }

    // 绘制正方形的ROI区域
    private void drawSquareROI(Canvas canvas, float[] roi, Paint paint) {
        // 计算ROI中心点
        float centerX = (roi[0] + (roi[2] - roi[0]) / 2) * viewWidth;
        float centerY = (roi[1] + (roi[3] - roi[1]) / 2) * viewHeight;

        // 计算ROI原始宽度和高度
        float roiWidth = (roi[2] - roi[0]) * viewWidth;
        float roiHeight = (roi[3] - roi[1]) * viewHeight;

        // 确定正方形的边长（使用较大的维度）
        float size = Math.max(roiWidth, roiHeight);

        // 绘制正方形
        canvas.drawRect(
                centerX - size / 2,
                centerY - size / 2,
                centerX + size / 2,
                centerY + size / 2,
                paint
        );
    }


    private void drawOneRect(Canvas canvas, int y, int x, Paint p) {
        // 定义正方形的大小
        int squareSize = 4; // 从之前的x+2到x-2变为4像素宽
        canvas.drawRect(x - 2, y - 2, x + 2, y + 2, p);
    }


    public void drawMy(Face d) {
        drawMy(d, -1, -1, -1, -1);
    }

    public void drawMy(Face d, int sx, int sy, int ex, int ey) {
        if (sx != -1) {
            this.sx = sx;
        }
        if (sy != -1) {
            this.sy = sy;
        }
        if (ex != -1) {
            this.ex = ex;
        }
        if (ey != -1) {
            this.ey = ey;
        }
        this.d = d;
        this.viewWidth = getMeasuredWidth();
        this.viewHeight = getMeasuredHeight();
        invalidate();
    }
}
