package com.example.smartbinapp;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class BinDetailActivity extends AppCompatActivity {

    private ImageView ivMainBin;
    private LinearLayout headerSection, controlButtons;
    private CardView statusCard, statsCard;
    private Button btnOpenBin, btnCheckStatus, btnSettings;

    private TextView tvBinCode, tvBinAddress, tvFillLevel, tvSensorStatus, tvNetworkStatus;
    private View viewFillStatus;

    private TextView tvOpenCount, tvEfficiency, tvWaste;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        loadBinData();
        startEntranceAnimations();
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

        tvBinCode = findViewById(R.id.tv_bin_code);
        tvBinAddress = findViewById(R.id.tv_bin_address);
        tvFillLevel = findViewById(R.id.tv_fill_level);
        tvSensorStatus = findViewById(R.id.tv_sensor_status);
        tvNetworkStatus = findViewById(R.id.tv_network_status);

        viewFillStatus = findViewById(R.id.view_fill_status);

        tvOpenCount = findViewById(R.id.tv_open_count);
        tvEfficiency = findViewById(R.id.tv_efficiency);
        tvWaste = findViewById(R.id.tv_waste);
    }

    private void loadBinData() {

        Intent intent = getIntent();
        if (intent == null) return;

        String binCode = intent.getStringExtra("binCode");
        double fill = intent.getDoubleExtra("fill", 0);
        String street = intent.getStringExtra("street");
        String ward = intent.getStringExtra("ward");
        String province = intent.getStringExtra("province");
        int sensor = intent.getIntExtra("status", 1); // 1 = ok, 0 = lỗi
        int network = intent.getIntExtra("network", 1);

        // HIỂN THỊ
        tvBinCode.setText("Thùng " + binCode);
        tvBinAddress.setText(street + ", " + ward + ", " + province);

        tvFillLevel.setText("Mức độ đầy: " + (int) fill + "%");
        tvSensorStatus.setText(sensor == 1 ? "Cảm biến: Hoạt động" : "Cảm biến: Lỗi");
        tvNetworkStatus.setText(network == 1 ? "Kết nối: WiFi" : "Kết nối: Mất mạng");

        tvOpenCount.setText("12");
        tvEfficiency.setText("85%");
        tvWaste.setText("2.5kg");
        if (sensor == 0) {
            Log.d("BIN_DETAIL", "⚠ Cảm biến lỗi → đổi icon GREY");
            ivMainBin.setImageResource(R.drawable.ic_bin_grey);
            viewFillStatus.setBackgroundColor(Color.parseColor("#9E9E9E")); // grey
            return; // ⚠ Không chạy tiếp logic fill nữa
        }

        // ĐỔI ICON + MÀU
        if (fill >= 80) {
            ivMainBin.setImageResource(R.drawable.ic_bin_red);
            viewFillStatus.setBackgroundColor(Color.parseColor("#E53935"));
        } else if (fill >= 40) {
            ivMainBin.setImageResource(R.drawable.ic_bin_yellow);
            viewFillStatus.setBackgroundColor(Color.parseColor("#FBC02D"));
        } else {
            ivMainBin.setImageResource(R.drawable.ic_bin_green);
            viewFillStatus.setBackgroundColor(Color.parseColor("#43A047"));
        }

        if (sensor == 0) {
            ivMainBin.setImageResource(R.drawable.ic_bin_grey);
            viewFillStatus.setBackgroundColor(Color.parseColor("#E53935"));
        }
    }

    private void startEntranceAnimations() {

        // Header fade
        ObjectAnimator a1 = ObjectAnimator.ofFloat(headerSection, "alpha", 0f, 1f);
        a1.setDuration(900);
        a1.start();

        // Status card fade
        ObjectAnimator a2 = ObjectAnimator.ofFloat(statusCard, "alpha", 0f, 1f);
        a2.setDuration(900);
        a2.setStartDelay(300);
        a2.start();

        // Control buttons fade
        ObjectAnimator a3 = ObjectAnimator.ofFloat(controlButtons, "alpha", 0f, 1f);
        a3.setDuration(900);
        a3.setStartDelay(600);
        a3.start();

        // Stats card fade
        ObjectAnimator a4 = ObjectAnimator.ofFloat(statsCard, "alpha", 0f, 1f);
        a4.setDuration(900);
        a4.setStartDelay(900);
        a4.start();

        // Icon rotation
        ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(ivMainBin, "rotationY", 0f, 360f);
        rotationAnimator.setDuration(1500);
        rotationAnimator.setStartDelay(700);
        rotationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        rotationAnimator.start();
    }

    private void setupButtonListeners() {

        btnOpenBin.setOnClickListener(v -> {
            Toast.makeText(this, "Báo cáo thùng rác...", Toast.LENGTH_SHORT).show();
        });

        btnCheckStatus.setOnClickListener(v -> {
            Toast.makeText(this, "Đang mở bản đồ...", Toast.LENGTH_SHORT).show();
        });

        btnSettings.setOnClickListener(v -> {
            Toast.makeText(this, "Cài đặt thùng...", Toast.LENGTH_SHORT).show();
        });
    }
}
