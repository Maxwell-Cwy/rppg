package com.example.myapplication;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import androidx.core.content.ContextCompat;          // 新增这行！
import com.example.myapplication.utils.TimeUtils;
import java.io.File;
import java.io.IOException;

/**
 * 视频录制服务（使用 Camera2 + MediaRecorder 推荐方案，但你当前想快速跑通）
 * 这里先用你原来的旧 Camera API，但全部修复编译错误
 */
public class VideoRecorder implements SurfaceHolder.Callback {

    private static final String TAG = "VideoRecorder";

    private final Context mContext;
    private final VideoListener mListener;
    private final SurfaceView mSurfaceView;
    private final SurfaceHolder mSurfaceHolder;

    private MediaRecorder mMediaRecorder;
    private android.hardware.Camera mCamera;   // 旧 API，必须写完整包名避免冲突

    private boolean isRecording = false;
    private String mVideoPath;

    public interface VideoListener {
        void onVideoStarted(String videoPath, String startTime);
        void onVideoFinished(String videoPath);
        void onVideoError(String errorMsg);
    }

    public VideoRecorder(Context context, VideoListener listener, SurfaceView surfaceView) {
        this.mContext = context;
        this.mListener = listener;
        this.mSurfaceView = surfaceView;
        this.mSurfaceHolder = surfaceView.getHolder();
        this.mSurfaceHolder.addCallback(this);
        // 兼容老设备
        this.mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void startRecording(long duration) {
        if (isRecording) {
            mListener.onVideoError("正在录制中，请勿重复点击");
            return;
        }

        // 权限检查（修复你报错的核心！）
        if (ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            mListener.onVideoError("缺少相机权限");
            return;
        }
        if (ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            mListener.onVideoError("缺少麦克风权限");
            return;
        }

        try {
            mCamera = android.hardware.Camera.open();
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();

            mMediaRecorder = new MediaRecorder();
            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setVideoSize(1280, 720);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoEncodingBitRate(5_000_000);
            mMediaRecorder.setOrientationHint(getCameraOrientation());

            mVideoPath = getVideoSavePath();
            mMediaRecorder.setOutputFile(mVideoPath);
            mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

            mMediaRecorder.prepare();
            mMediaRecorder.start();

            isRecording = true;
            String startTime = TimeUtils.getPreciseTimeStamp();
            mListener.onVideoStarted(mVideoPath, startTime);

            // 90秒后自动停止
            new android.os.Handler().postDelayed(this::stopRecording, duration);

        } catch (Exception e) {
            e.printStackTrace();
            mListener.onVideoError("录制失败：" + e.getMessage());
            releaseResources();
        }
    }

    public void stopRecording() {
        if (!isRecording) return;

        try {
            mMediaRecorder.stop();
            mListener.onVideoFinished(mVideoPath);
        } catch (Exception e) {
            e.printStackTrace();
            mListener.onVideoError("停止失败：" + e.getMessage());
        } finally {
            releaseResources();
            isRecording = false;
        }
    }

    private int getCameraOrientation() {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK, info);
        return info.orientation;
    }

    private String getVideoSavePath() {
        File dir = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "OximeterVideos");
        if (!dir.exists()) dir.mkdirs();
        String fileName = "Oximeter_" + TimeUtils.getFileNameTimeStamp() + ".mp4";
        return new File(dir, fileName).getAbsolutePath();
    }

    public void releaseResources() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if (mCamera != null) {
            mCamera.lock();
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public String getCurrentVideoPath() {
        return mVideoPath;
    }

    // SurfaceHolder.Callback
    @Override public void surfaceCreated(SurfaceHolder holder) {}
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        if (isRecording) stopRecording();
        releaseResources();
    }
}