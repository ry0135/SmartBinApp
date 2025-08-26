package com.example.smartbinapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private ImageView ivSmartBin;
    private TextView tvAppName, tvSubtitle;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize views
        ivSmartBin = findViewById(R.id.iv_smart_bin);
        tvAppName = findViewById(R.id.tv_app_name);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        progressBar = findViewById(R.id.progress_bar);

        // Start animations
        startAnimations();
    }

    private void startAnimations() {
        // Animate smart bin icon
        ObjectAnimator binAnimator = ObjectAnimator.ofFloat(ivSmartBin, "alpha", 0f, 1f);
        binAnimator.setDuration(1000);
        binAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        binAnimator.start();

        // Animate app name
        ObjectAnimator nameAnimator = ObjectAnimator.ofFloat(tvAppName, "alpha", 0f, 1f);
        nameAnimator.setDuration(800);
        nameAnimator.setStartDelay(500);
        nameAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        nameAnimator.start();

        // Animate subtitle
        ObjectAnimator subtitleAnimator = ObjectAnimator.ofFloat(tvSubtitle, "alpha", 0f, 1f);
        subtitleAnimator.setDuration(800);
        subtitleAnimator.setStartDelay(800);
        subtitleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        subtitleAnimator.start();

        // Animate progress bar
        ObjectAnimator progressAnimator = ObjectAnimator.ofFloat(progressBar, "alpha", 0f, 1f);
        progressAnimator.setDuration(600);
        progressAnimator.setStartDelay(1000);
        progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnimator.start();

        // Animate progress bar fill
        ValueAnimator progressFillAnimator = ValueAnimator.ofInt(0, 100);
        progressFillAnimator.setDuration(2000);
        progressFillAnimator.setStartDelay(1200);
        progressFillAnimator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            progressBar.setProgress(progress);
        });
        progressFillAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Navigate to login activity after animation completes
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    finish();
                }, 500);
            }
        });
        progressFillAnimator.start();
    }
} 