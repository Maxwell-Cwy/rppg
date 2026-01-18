package com.example.myapplication.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class VerticalMarqueeView extends FrameLayout {
    private List<TextView> textViews = new ArrayList<>();
    private int currentIndex = 0;
    private static final int SCROLL_INTERVAL = 3000;

    public VerticalMarqueeView(Context context) {
        this(context, null);
    }

    public VerticalMarqueeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalMarqueeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // 使用FrameLayout作为容器，让所有子视图堆叠在一起
        setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));
        for (int i = 0; i < 3; i++) {
            TextView textView = getTextView(context);
            textViews.add(textView);
            addView(textView);
        }

        // 默认显示第一个TextView
        if (!textViews.isEmpty()) {
            textViews.get(0).setVisibility(View.VISIBLE);
        }
    }

    private static @NonNull TextView getTextView(Context context) {
        TextView textView = new TextView(context);
        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        textView.setLayoutParams(params);
        textView.setSingleLine(true);
        textView.setTextColor(0xff0e7dec);
        textView.setTextSize(23);
        textView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textView.setVisibility(View.INVISIBLE);
        return textView;
    }

    public void setTexts(List<String> texts) {
        for (int i = 0; i < Math.min(texts.size(), 3); i++) {
            textViews.get(i).setText(texts.get(i));
        }
    }

    public void startAutoScroll() {
        removeCallbacks(scrollRunnable);
        postDelayed(scrollRunnable, SCROLL_INTERVAL);
    }

    public void stopAutoScroll() {
        removeCallbacks(scrollRunnable);
    }

    private Runnable scrollRunnable = new Runnable() {
        @Override
        public void run() {
            scrollUp();
            postDelayed(this, SCROLL_INTERVAL);
        }
    };

    private void scrollUp() {
        for (TextView textView : textViews) {
            textView.setVisibility(View.INVISIBLE);
            textView.clearAnimation();
        }
        currentIndex = (currentIndex + 1) % textViews.size();
        final TextView currentTextView = textViews.get(currentIndex);
        currentTextView.setVisibility(View.VISIBLE);
        final int height = getHeight();
        if (height <= 0) {
            post(new Runnable() {
                @Override
                public void run() {
                    scrollUp();
                }
            });
            return;
        }
        Animation slideIn = new TranslateAnimation(
                0, 0, height, 0
        );
        slideIn.setDuration(500);
        slideIn.setFillAfter(true);
        currentTextView.startAnimation(slideIn);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAutoScroll();
    }

}
