package com.example.smartbinapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvEmail;
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
    }
}
