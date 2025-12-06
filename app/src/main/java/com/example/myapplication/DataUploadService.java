package com.example.myapplication;

import android.content.Context;
import android.os.AsyncTask;
import com.example.myapplication.model.DetectionTimeStamp;
import com.example.myapplication.model.OximeterData;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 * 数据上传服务：上传蓝牙数据、视频文件、时间戳到后端
 */
public class DataUploadService {
    // 后端API地址（请替换为你的实际后端地址）
    private static final String UPLOAD_API_URL = "http://your-backend-domain.com/api/oximeter/upload";
    // 边界符（文件上传用）
    private static final String BOUNDARY = UUID.randomUUID().toString();
    // 换行符
    private static final String LINE_END = "\r\n";
    // 内容类型
    private static final String CONTENT_TYPE = "multipart/form-data; boundary=" + BOUNDARY;

    // 上下文
    private final Context mContext;

    // 上传回调接口
    public interface UploadListener {
        // 上传成功
        void onUploadSuccess(String response);
        // 上传失败
        void onUploadFailed(String errorMsg);
        // 上传进度
        void onUploadProgress(int progress);
    }

    // 构造方法
    public DataUploadService(Context context) {
        this.mContext = context;
    }

    /**
     * 上传所有数据（蓝牙数据+视频+时间戳）
     */
    public void uploadAllData(OximeterData oximeterData, String videoPath,
                              DetectionTimeStamp timeStamp, UploadListener listener) {
        // 检查数据完整性
        if (!oximeterData.hasData()) {
            listener.onUploadFailed("蓝牙数据为空，无法上传");
            return;
        }

        if (videoPath == null || videoPath.isEmpty() || !new File(videoPath).exists()) {
            listener.onUploadFailed("视频文件不存在，无法上传");
            return;
        }

        if (timeStamp.getBluetoothConnectTime() == null || timeStamp.getVideoStartTime() == null) {
            listener.onUploadFailed("时间戳不完整，无法上传");
            return;
        }

        // 异步上传（避免阻塞主线程）
        new UploadTask(oximeterData, videoPath, timeStamp, listener).execute();
    }

    /**
     * 异步上传任务（AsyncTask）
     */
    private class UploadTask extends AsyncTask<Void, Integer, String> {
        private final OximeterData mOximeterData;
        private final String mVideoPath;
        private final DetectionTimeStamp mTimeStamp;
        private final UploadListener mListener;

        // 构造方法
        public UploadTask(OximeterData oximeterData, String videoPath,
                          DetectionTimeStamp timeStamp, UploadListener listener) {
            this.mOximeterData = oximeterData;
            this.mVideoPath = videoPath;
            this.mTimeStamp = timeStamp;
            this.mListener = listener;
        }

        // 后台执行上传
        @Override
        protected String doInBackground(Void... voids) {
            try {
                // 创建URL连接
                URL url = new URL(UPLOAD_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // 设置请求属性
                connection.setRequestMethod("POST");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Charset", "UTF-8");
                connection.setRequestProperty("Content-Type", CONTENT_TYPE);

                // 获取输出流（写入请求体）
                DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());

                // 1. 写入蓝牙数据（JSON格式）
                writeDataPart(outputStream, "oximeter_data", createOximeterDataJson().getBytes("UTF-8"));

                // 2. 写入时间戳数据（JSON格式）
                writeDataPart(outputStream, "time_stamp", createTimeStampJson().getBytes("UTF-8"));

                // 3. 写入视频文件（带进度）
                writeFilePart(outputStream, "video_file", new File(mVideoPath));

                // 4. 写入结束边界
                outputStream.writeBytes("--" + BOUNDARY + "--" + LINE_END);
                outputStream.flush();
                outputStream.close();

                // 检查响应码
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // 读取响应内容
                    String response = readInputStream(connection.getInputStream());
                    connection.disconnect();
                    return "success|" + response;
                } else {
                    String error = "服务器响应错误，状态码：" + responseCode;
                    connection.disconnect();
                    return "error|" + error;
                }

            } catch (Exception e) {
                e.printStackTrace();
                return "error|" + e.getMessage();
            }
        }

        // 进度更新（主线程）
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (values != null && values.length > 0) {
                mListener.onUploadProgress(values[0]);
            }
        }

        // 上传完成（主线程）
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result.startsWith("success|")) {
                String response = result.substring("success|".length());
                mListener.onUploadSuccess(response);
            } else if (result.startsWith("error|")) {
                String errorMsg = result.substring("error|".length());
                mListener.onUploadFailed(errorMsg);
            } else {
                mListener.onUploadFailed("未知错误");
            }
        }

        /**
         * 创建血氧数据JSON字符串
         */
        /**
         * 创建血氧数据JSON字符串（已适配你最新的 OximeterData.java）
         */
        private String createOximeterDataJson() throws JSONException {
            JSONObject json = new JSONObject();

            // 实时值（最新一包数据）
            json.put("spo2", mOximeterData.getSpo2());           // 实时血氧
            json.put("pulse_rate", mOximeterData.getPr());        // 实时心率
            json.put("temperature", mOximeterData.getTemperature());
            json.put("pi", mOximeterData.getPi());
            json.put("respiration_rate", mOximeterData.getRespirationRate());
            json.put("probe_status", mOximeterData.getProbeStatus());
            json.put("battery_level", mOximeterData.getBatteryLevel());

            // 统计值（全程平均、最低、最高）
            json.put("avg_spo2", mOximeterData.getAvgSpo2());
            json.put("min_spo2", mOximeterData.getMinSpo2());
            json.put("max_spo2", mOximeterData.getMaxSpo2());
            json.put("avg_pr", mOximeterData.getAvgPr());
            json.put("min_pr", mOximeterData.getMinPr());
            json.put("max_pr", mOximeterData.getMaxPr());

            // 原始数据（可选：如果后端要原始波形）
            json.put("raw_data_count", mOximeterData.getCount());
            json.put("start_time", mOximeterData.getStartTime());

            return json.toString();
        }

        /**
         * 创建时间戳JSON字符串
         */
        private String createTimeStampJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("bluetooth_connect_time", mTimeStamp.getBluetoothConnectTime());
            json.put("video_start_time", mTimeStamp.getVideoStartTime());
            json.put("data_start_time", mTimeStamp.getBluetoothDataStartTime());
            json.put("upload_time", com.example.myapplication.utils.TimeUtils.getPreciseTimeStamp());
            return json.toString();
        }

        /**
         * 写入普通数据部分（非文件）
         */
        private void writeDataPart(DataOutputStream outputStream, String key, byte[] data) throws IOException {
            outputStream.writeBytes("--" + BOUNDARY + LINE_END);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + LINE_END);
            outputStream.writeBytes("Content-Type: application/json; charset=UTF-8" + LINE_END);
            outputStream.writeBytes(LINE_END);
            outputStream.write(data);
            outputStream.writeBytes(LINE_END);
        }

        /**
         * 写入文件部分（带进度）
         */
        private void writeFilePart(DataOutputStream outputStream, String key, File file) throws IOException {
            String fileName = file.getName();
            // 文件头
            outputStream.writeBytes("--" + BOUNDARY + LINE_END);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + fileName + "\"" + LINE_END);
            outputStream.writeBytes("Content-Type: video/mp4" + LINE_END); // 视频MIME类型
            outputStream.writeBytes(LINE_END);

            // 读取文件并写入（计算进度）
            FileInputStream inputStream = new FileInputStream(file);
            byte[] buffer = new byte[4096]; // 4KB缓冲区
            long totalBytes = file.length();
            long uploadedBytes = 0;
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                uploadedBytes += bytesRead;
                // 计算进度（百分比）
                int progress = (int) ((uploadedBytes * 100) / totalBytes);
                // 发布进度（主线程更新）
                publishProgress(progress);
            }

            // 关闭文件流
            inputStream.close();
            outputStream.writeBytes(LINE_END);
        }

        /**
         * 读取输入流（响应内容）
         */
        private String readInputStream(InputStream inputStream) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        }
    }
}