package com.example.smartbinapp;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private ImageView ivLoginBin;
    private LinearLayout headerSection, quickLoginSection;
    private CardView loginCard;
    private TextInputEditText etUsername, etPassword;
    private TextInputLayout tilUsername, tilPassword;
    private Button btnLogin, btnGuestLogin, btnDemoLogin;
    private TextView tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        initializeViews();
        
        // Start entrance animations
        startEntranceAnimations();
        
        // Set up button click listeners
        setupButtonListeners();
    }

    private void initializeViews() {
        ivLoginBin = findViewById(R.id.iv_login_bin);
        headerSection = findViewById(R.id.header_section);
        loginCard = findViewById(R.id.login_card);
        quickLoginSection = findViewById(R.id.quick_login_section);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        tilUsername = findViewById(R.id.til_username);
        tilPassword = findViewById(R.id.til_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGuestLogin = findViewById(R.id.btn_guest_login);
        btnDemoLogin = findViewById(R.id.btn_demo_login);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
    }

    private void startEntranceAnimations() {
        // Animate header section
        ObjectAnimator headerAnimator = ObjectAnimator.ofFloat(headerSection, "alpha", 0f, 1f);
        headerAnimator.setDuration(1000);
        headerAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        headerAnimator.start();

        // Animate login card with delay
        ObjectAnimator loginCardAnimator = ObjectAnimator.ofFloat(loginCard, "alpha", 0f, 1f);
        loginCardAnimator.setDuration(800);
        loginCardAnimator.setStartDelay(300);
        loginCardAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        loginCardAnimator.start();

        // Animate quick login section with delay
        ObjectAnimator quickLoginAnimator = ObjectAnimator.ofFloat(quickLoginSection, "alpha", 0f, 1f);
        quickLoginAnimator.setDuration(800);
        quickLoginAnimator.setStartDelay(600);
        quickLoginAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        quickLoginAnimator.start();

        // Animate smart bin icon with rotation
        ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(ivLoginBin, "rotationY", 0f, 360f);
        rotationAnimator.setDuration(1500);
        rotationAnimator.setStartDelay(500);
        rotationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        rotationAnimator.start();
    }

    private void setupButtonListeners() {
        btnLogin.setOnClickListener(v -> {
            animateButtonClick(v);
            performLogin();
        });

        btnGuestLogin.setOnClickListener(v -> {
            animateButtonClick(v);
            loginAsGuest();
        });

        btnDemoLogin.setOnClickListener(v -> {
            animateButtonClick(v);
            loginAsDemo();
        });

        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        });

        // Add register button click listener
        findViewById(R.id.btn_register).setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty()) {
            tilUsername.setError("Vui lòng nhập tên đăng nhập");
            return;
        }

        if (password.isEmpty()) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            return;
        }

        // Simple validation - in real app, you would validate against server
        if (username.equals("admin") && password.equals("123456")) {
            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
            navigateToMain();
        } else {
            Toast.makeText(this, "Tên đăng nhập hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show();
        }
    }

    private void loginAsGuest() {
        Toast.makeText(this, "Đăng nhập với tài khoản khách", Toast.LENGTH_SHORT).show();
        navigateToMain();
    }

    private void loginAsDemo() {
        Toast.makeText(this, "Chế độ demo", Toast.LENGTH_SHORT).show();
        navigateToMain();
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
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