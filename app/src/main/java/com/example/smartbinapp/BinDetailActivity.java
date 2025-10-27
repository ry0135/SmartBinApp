package com.example.smartbinapp;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class BinDetailActivity extends AppCompatActivity {

    private ImageView ivMainBin;
    private LinearLayout headerSection, controlButtons;
    private CardView statusCard, statsCard;
    private Button btnOpenBin, btnCheckStatus, btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        initializeViews();

        // Start entrance animations
        startEntranceAnimations();

        // Set up button click listeners
        setupButtonListeners();
    }

    private void initializeViews() {
        ivMainBin = findViewById(R.id.iv_main_bin);
        headerSection = findViewById(R.id.header_section);
        statusCard = findViewById(R.id.status_card);
        controlButtons = findViewById(R.id.control_buttons);
        statsCard = findViewById(R.id.stats_card);
        btnOpenBin = findViewById(R.id.btn_open_bin);
        btnCheckStatus = findViewById(R.id.btn_check_status);
        btnSettings = findViewById(R.id.btn_settings);
    }

    private void startEntranceAnimations() {
        // Animate header section
        ObjectAnimator headerAnimator = ObjectAnimator.ofFloat(headerSection, "alpha", 0f, 1f);
        headerAnimator.setDuration(1000);
        headerAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        headerAnimator.start();

        // Animate status card with delay
        ObjectAnimator statusAnimator = ObjectAnimator.ofFloat(statusCard, "alpha", 0f, 1f);
        statusAnimator.setDuration(800);
        statusAnimator.setStartDelay(300);
        statusAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        statusAnimator.start();

        // Animate control buttons with delay
        ObjectAnimator buttonsAnimator = ObjectAnimator.ofFloat(controlButtons, "alpha", 0f, 1f);
        buttonsAnimator.setDuration(800);
        buttonsAnimator.setStartDelay(600);
        buttonsAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        buttonsAnimator.start();

        // Animate stats card with delay
        ObjectAnimator statsAnimator = ObjectAnimator.ofFloat(statsCard, "alpha", 0f, 1f);
        statsAnimator.setDuration(800);
        statsAnimator.setStartDelay(900);
        statsAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        statsAnimator.start();

        // Animate smart bin icon with rotation
        ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(ivMainBin, "rotationY", 0f, 360f);
        rotationAnimator.setDuration(1500);
        rotationAnimator.setStartDelay(500);
        rotationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        rotationAnimator.start();
    }

    private void setupButtonListeners() {
        btnOpenBin.setOnClickListener(v -> {
            animateButtonClick(v);
            Toast.makeText(this, "Đang mở thùng rác...", Toast.LENGTH_SHORT).show();
        });

        btnCheckStatus.setOnClickListener(v -> {
            animateButtonClick(v);
            Toast.makeText(this, "Đang kiểm tra trạng thái...", Toast.LENGTH_SHORT).show();
        });

        btnSettings.setOnClickListener(v -> {
            animateButtonClick(v);
            Toast.makeText(this, "Mở cài đặt...", Toast.LENGTH_SHORT).show();
        });
    }

    private void animateButtonClick(View view) {
        // Scale down animation
        ObjectAnimator scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f);
        scaleDown.setDuration(100);
        scaleDown.setInterpolator(new AccelerateDecelerateInterpolator());

        // Scale up animation
        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f);
        scaleUp.setDuration(100);
        scaleUp.setStartDelay(100);
        scaleUp.setInterpolator(new AccelerateDecelerateInterpolator());

        scaleDown.start();
        scaleUp.start();
    }
}
