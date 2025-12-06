package com.example.myapplication;
import android.widget.Toast;
import android.content.Context;
import android.util.Log;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.example.myapplication.model.DetectionTimeStamp;
import com.example.myapplication.model.OximeterData;
import com.example.myapplication.utils.DataSaver;
import com.example.myapplication.utils.TimeUtils;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;

import android.os.Environment;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class VideoRecorder {

    private final Context context;
    private final VideoListener listener;
    private final PreviewView previewView;

    private VideoCapture<Recorder> videoCapture;
    private Recording currentRecording;



    public interface VideoListener {
        void onVideoStarted(String videoPath, String startTime);
        void onVideoFinished(String videoPath);
        void onVideoError(String errorMsg);
    }

    public VideoRecorder(Context context, VideoListener listener, PreviewView previewView) {
        this.context = context;
        this.listener = listener;
        this.previewView = previewView;
    }


    public void startRecording(long durationMillis) {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(context);
        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        (LifecycleOwner) context,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        videoCapture
                );

                File dir = new File(context.getExternalFilesDir("Movies"), "OximeterVideos");
                if (!dir.exists()) dir.mkdirs();
                File videoFile = new File(dir, "Oximeter_" + TimeUtils.getFileNameTimeStamp() + ".mp4");

                FileOutputOptions options = new FileOutputOptions.Builder(videoFile).build();

                currentRecording = videoCapture.getOutput()
                        .prepareRecording(context, options)
                        .start(ContextCompat.getMainExecutor(context), recordEvent -> {
                            if (recordEvent instanceof VideoRecordEvent.Start) {
                                listener.onVideoStarted(videoFile.getAbsolutePath(), TimeUtils.getPreciseTimeStamp());
                                new android.os.Handler(android.os.Looper.getMainLooper())
                                        .postDelayed(this::stopRecording, durationMillis);

                            } else if (recordEvent instanceof VideoRecordEvent.Finalize) {
                                VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) recordEvent;
                                if (finalizeEvent.hasError()) {
                                    listener.onVideoError("录制失败：" + finalizeEvent.getError());
                                } else {
                                    String path = videoFile.getAbsolutePath();
                                    listener.onVideoFinished(path);
                                }
                                currentRecording = null;
                            }
                        });

            } catch (Exception e) {
                e.printStackTrace();
                listener.onVideoError("启动失败：" + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(context));
    }

    public void stopRecording() {
        if (currentRecording != null) {
            currentRecording.stop();
            currentRecording = null;
        }
    }

    public boolean isRecording() {
        return currentRecording != null;
    }

    public void releaseResources() {
        stopRecording();
    }


}