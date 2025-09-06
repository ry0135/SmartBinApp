package com.example.smartbinapp;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.smartbinapp.model.Account;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ImageView ivLoginBin;
    private LinearLayout headerSection, quickLoginSection;
    private CardView loginCard;
    private TextInputEditText etUsername, etPassword;
    private TextInputLayout tilUsername, tilPassword;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Khởi tạo View
        initializeViews();

        // Khởi tạo Retrofit
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // Animation khi mở màn hình
        startEntranceAnimations();

        // Gán sự kiện click
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
    }

    // ================== ANIMATIONS ==================
    private void startEntranceAnimations() {
        // Header fade in
        ObjectAnimator headerAnimator = ObjectAnimator.ofFloat(headerSection, "alpha", 0f, 1f);
        headerAnimator.setDuration(1000);
        headerAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        headerAnimator.start();

        // Login card fade in
        ObjectAnimator loginCardAnimator = ObjectAnimator.ofFloat(loginCard, "alpha", 0f, 1f);
        loginCardAnimator.setDuration(800);
        loginCardAnimator.setStartDelay(300);
        loginCardAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        loginCardAnimator.start();

        // Quick login fade in
        ObjectAnimator quickLoginAnimator = ObjectAnimator.ofFloat(quickLoginSection, "alpha", 0f, 1f);
        quickLoginAnimator.setDuration(800);
        quickLoginAnimator.setStartDelay(600);
        quickLoginAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        quickLoginAnimator.start();

        // Icon xoay
        ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(ivLoginBin, "rotationY", 0f, 360f);
        rotationAnimator.setDuration(1500);
        rotationAnimator.setStartDelay(500);
        rotationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        rotationAnimator.start();
    }

    private void animateButtonClick(View view) {
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f);
        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);

        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f);
        scaleUpX.setDuration(100);
        scaleUpY.setDuration(100);
        scaleUpX.setStartDelay(100);
        scaleUpY.setStartDelay(100);

        scaleDownX.start();
        scaleDownY.start();
        scaleUpX.start();
        scaleUpY.start();
    }
    // =================================================

    private void setupButtonListeners() {
        findViewById(R.id.btn_login).setOnClickListener(v -> {
            animateButtonClick(v);
            performLogin();
        });

        findViewById(R.id.btn_register).setOnClickListener(v -> {
            animateButtonClick(v);
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        findViewById(R.id.tv_forgot_password).setOnClickListener(v ->
                Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
        );
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

        Account account = new Account();
        account.setEmail(username);
        account.setPassword(password);

        apiService.login(account).enqueue(new Callback<Account>() {
            @Override
            public void onResponse(Call<Account> call, Response<Account> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Account account = response.body();

                    // Lưu session
                    SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("userId", String.valueOf(account.getAccountId()));
                    editor.putString("userName", account.getFullName());
                    editor.apply();

                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                    // Chuyển sang HomeActivity
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    intent.putExtra("firstname", account.getFullName());
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    finish();

                } else {
                    Toast.makeText(LoginActivity.this, "Sai thông tin đăng nhập", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Account> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
