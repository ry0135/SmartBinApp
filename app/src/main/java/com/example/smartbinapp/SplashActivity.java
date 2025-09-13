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
        // Fade in icon
        ivSmartBin.setAlpha(0f);
        ivSmartBin.animate()
                .alpha(1f)
                .setDuration(800)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Fade in texts
        tvAppName.setAlpha(0f);
        tvSubtitle.setAlpha(0f);
        tvAppName.animate().alpha(1f).setDuration(800).setStartDelay(400).start();
        tvSubtitle.animate().alpha(1f).setDuration(800).setStartDelay(600).start();

        // Fake loading bar (không cần animate từng progress để tránh block UI)
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);

        // Chỉ delay 2s rồi chuyển sang Login
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        }, 2000);
    }

} 