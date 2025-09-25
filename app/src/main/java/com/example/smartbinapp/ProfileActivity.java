package com.example.smartbinapp;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvEmail;
    private ImageView ivMenu;
    private LinearLayout btnHome,btnReport, btnShowTask, btnAccount;
    private ImageView ivAvatar;
    private LinearLayout itemEditProfile, itemChangePassword, itemNotification, itemHelp, itemLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Ánh xạ view
        ivAvatar = findViewById(R.id.ivAvatar);
        tvUserName = findViewById(R.id.tvUserName);
        tvEmail = findViewById(R.id.tvEmail);

        itemEditProfile = findViewById(R.id.itemEditProfile);
        itemChangePassword = findViewById(R.id.itemChangePassword);
        itemNotification = findViewById(R.id.itemNotification);
        itemHelp = findViewById(R.id.itemHelp);
        itemLogout = findViewById(R.id.itemLogout);

        // Lấy thông tin user từ SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userName = prefs.getString("userName", "Người dùng");
        String email = prefs.getString("email", "example@email.com");

        tvUserName.setText(userName);
        tvEmail.setText(email);

        // Sửa thông tin
        itemEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                startActivity(intent);
        });

        // Đổi mật khẩu
        itemChangePassword.setOnClickListener(v -> {
            Toast.makeText(this, "Mở màn đổi mật khẩu", Toast.LENGTH_SHORT).show();
            // startActivity(new Intent(ProfileActivity.this, ChangePasswordActivity.class));
        });

        // Cài đặt thông báo
        itemNotification.setOnClickListener(v -> {
            Toast.makeText(this, "Mở cài đặt thông báo", Toast.LENGTH_SHORT).show();
            // startActivity(new Intent(ProfileActivity.this, NotificationSettingsActivity.class));
        });

        // Trợ giúp
        itemHelp.setOnClickListener(v -> {
            Toast.makeText(this, "Mở trợ giúp & hỗ trợ", Toast.LENGTH_SHORT).show();
            // startActivity(new Intent(ProfileActivity.this, HelpActivity.class));
        });

        // Đăng xuất
        itemLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear(); // Xóa session
            editor.apply();

            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });


        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        ivMenu = findViewById(R.id.iv_menu);
        btnHome = findViewById(R.id.btn_home);
        btnShowTask = findViewById(R.id.btn_showtask);
        btnAccount = findViewById(R.id.btn_account);
        btnReport = findViewById(R.id.btn_report);
    }

    private void setupClickListeners() {
        ivMenu  .setOnClickListener(v -> {
            animateButtonClick(v);
            Toast.makeText(this, "Menu", Toast.LENGTH_SHORT).show();
        });

        btnHome.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, true);
            setActiveTab(btnReport, false);
            setActiveTab(btnShowTask, false);
            setActiveTab(btnAccount, false);
        });

        btnReport.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, false);
            setActiveTab(btnReport, true);
            setActiveTab(btnShowTask, false);
            setActiveTab(btnAccount, false);
            Intent intent = new Intent(ProfileActivity.this, TaskSummaryActivity.class);
            startActivity(intent);
            finish();
        });

        btnShowTask.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, false);
            setActiveTab(btnShowTask, true);
            setActiveTab(btnReport, false);
            setActiveTab(btnAccount, false);
            Intent intent = new Intent(ProfileActivity.this, TaskSummaryActivity.class);
            startActivity(intent);
            finish();
        });
        btnAccount.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, false);
            setActiveTab(btnReport, false);
            setActiveTab(btnShowTask, false);
            setActiveTab(btnAccount, true);
            Intent intent = new Intent(ProfileActivity.this, ProfileActivity.class);
            startActivity(intent);
            finish();
        });
    }


    private void setActiveTab(LinearLayout tab, boolean isActive) {
        ImageView icon = (ImageView) tab.getChildAt(0);
        TextView text = (TextView) tab.getChildAt(1);

        int activeColor = getResources().getColor(android.R.color.holo_green_dark);
        int inactiveColor = getResources().getColor(android.R.color.darker_gray);

        icon.setColorFilter(isActive ? activeColor : inactiveColor);
        text.setTextColor(isActive ? activeColor : inactiveColor);
    }

    private void animateButtonClick(View view) {
        ObjectAnimator scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f);
        scaleDown.setDuration(100);
        scaleDown.start();

        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f);
        scaleUp.setDuration(100);
        scaleUp.setStartDelay(100);
        scaleUp.start();
    }


}
