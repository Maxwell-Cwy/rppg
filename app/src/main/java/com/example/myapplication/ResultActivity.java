package com.example.myapplication;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.google.gson.Gson;
import com.wuzhengai.examination.JSONBean.auto.PGGLSTMBean;
import java.util.List;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        LineChart hrvTrendChart = findViewById(R.id.hrv_trend_chart);
        TextView tvHrvTrendDesc = findViewById(R.id.tv_hrv_trend_desc);
        TextView tvHrvKnowledge = findViewById(R.id.tv_hrv_knowledge);
        TextView tvHeartBrainRisk = findViewById(R.id.tv_heart_brain_risk);
        TextView tvSleepStatus = findViewById(R.id.tv_sleep_status);

        //        TextView tvResult = findViewById(R.id.tv_result);
//        String report = getIntent().getStringExtra("REPORT");
//        String videoPath = getIntent().getStringExtra("VIDEO_PATH");
//
//        StringBuilder sb = new StringBuilder();
//        sb.append("✅ 检测已完成！\n\n");
//        if (report != null) sb.append(report).append("\n\n");
//        if (videoPath != null) sb.append("视频路径：").append(videoPath);
//
//        tvResult.setText(sb.toString());

        String json = getIntent().getStringExtra("PGG_LSTM_JSON");
        if (json == null) {
            finish();
            return;
        }

        PGGLSTMBean bean = new Gson().fromJson(json, PGGLSTMBean.class);
        renderReport(bean);
    }

    private void renderReport(PGGLSTMBean bean) {
        // 1. 心率
        setText(R.id.tv_heart_rate, formatInt(bean.getResultHeart(), "次/分"));

        // 2. 脉搏
        setText(R.id.tv_pulse_rate, formatInt(bean.getResultPulse(), "次/分"));

        // 3. 血氧
        setText(R.id.tv_spo2, formatInt(bean.getResultSpo2(), "%"));

        // 4. 呼吸率
        setText(R.id.tv_respiratory, formatInt(bean.getResultRespiratory(), "次/分"));

        // 5. 血压 (resultBpm 是 List<Float>)
        String bpText = "--";
        List<Float> bpm = bean.getResultBpm();
        if (bpm != null && bpm.size() >= 2) {
            int sbp = Math.round(bpm.get(0)); // 收缩压
            int dbp = Math.round(bpm.get(1)); // 舒张压
            bpText = sbp + "/" + dbp + " mmHg";
        }
        setText(R.id.tv_blood_pressure, bpText);

        // 6. 房颤
        String afText = "--";
        Integer af = bean.getResultAf();
        if (af != null) {
            afText = (af == 1) ? "无" : "存在";
        }
        setText(R.id.tv_af, afText);

        // 7. HRV 核心指标（字符串类型，直接显示）
//        setText(R.id.tv_rmssd, bean.getResultRmssd() != null ? bean.getResultRmssd() + " ms" : "--");
//        setText(R.id.tv_sdnn, bean.getResultSdnn() != null ? bean.getResultSdnn() + " ms" : "--");
//        setText(R.id.tv_nn, bean.getResultNn() != null ? bean.getResultNn() + " ms" : "--");
//        setText(R.id.tv_pnn50, bean.getResultPnn50() != null ? bean.getResultPnn50() + "%" : "--");
        //8.酒精
        setText(R.id.tv_alcohol_pro, bean.getResult_alcohol_pro());
        //9.早搏
        setText(R.id.tv_premature_beat, bean.getResult_premature_beat());
        //10.心律不齐
        setText(R.id.tv_irregular_heartbeat, bean.getResult_irregular_heartbeat());
        //11.贫血
        setText(R.id.tv_anemia, bean.getResult_anemia());
        //12.血红蛋白
        setText(R.id.tv_hb, bean.getResult_hb());
    }

    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null) {
            tv.setText(text);
        }
    }

    private String formatInt(Integer value, String unit) {
        return (value != null) ? value + " " + unit : "--";
    }
}