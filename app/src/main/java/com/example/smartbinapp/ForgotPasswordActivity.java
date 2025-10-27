package com.example.smartbinapp;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputLayout tilEmail;
    private MaterialButton btnSendCode, btnBackLogin;
    private CircularProgressIndicator progressIndicator;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initViews();
        setupAnimations();
        setupClickListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        tilEmail = findViewById(R.id.til_email);
        btnSendCode = findViewById(R.id.btn_send_code);
        btnBackLogin = findViewById(R.id.btn_back_login);
        progressIndicator = findViewById(R.id.progress_indicator);

        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
    }

    private void setupAnimations() {
        // Animation khi mở activity
        new Handler().postDelayed(() -> {
            View header = findViewById(R.id.header_layout);
            if (header != null) {
                header.setAlpha(0f);
                header.animate()
                        .alpha(1f)
                        .setDuration(600)
                        .start();
            }
        }, 200);
    }

    private void setupClickListeners() {
        btnSendCode.setOnClickListener(v -> {
            animateButtonClick(v);
            sendVerificationCode();
        });

        btnBackLogin.setOnClickListener(v -> {
            animateButtonClick(v);
            finish();
        });

        // Clear error khi user bắt đầu nhập
        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                tilEmail.setError(null);
            }
        });

        etEmail.setOnKeyListener((v, keyCode, event) -> {
            tilEmail.setError(null);
            return false;
        });
    }

    private void sendVerificationCode() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            showError("Vui lòng nhập email");
            tilEmail.setError("Email không được để trống");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Email không hợp lệ");
            tilEmail.setError("Định dạng email không đúng");
            return;
        }

        // Show loading
        showLoading(true);

        // Gọi API
        apiService.forgotPassword(email).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                showLoading(false);

                if (response.isSuccessful()) {
                    showSuccess("Mã xác nhận đã được gửi đến email của bạn!");

                    new Handler().postDelayed(() -> {
                        Intent intent = new Intent(ForgotPasswordActivity.this, VerifyCodeActivity.class);
                        intent.putExtra("EMAIL", email);
                        intent.putExtra("MODE", "forgot");
                        startActivity(intent);
                        finish();
                    }, 1500);
                } else {
                    String errorMessage = "Email không tồn tại trong hệ thống";
                    if (response.code() == 500) {
                        errorMessage = "Lỗi server, vui lòng thử lại sau";
                    }
                    showError(errorMessage);
                    tilEmail.setError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showLoading(false);
                String errorMessage = "Lỗi kết nối: " + t.getMessage();
                showError(errorMessage);
                tilEmail.setError("Không thể kết nối đến server");
            }
        });

    }

    private void showLoading(boolean show) {
        if (show) {
            btnSendCode.setVisibility(View.INVISIBLE);
            progressIndicator.setVisibility(View.VISIBLE);
            btnSendCode.setEnabled(false);
            btnBackLogin.setEnabled(false);
        } else {
            btnSendCode.setVisibility(View.VISIBLE);
            progressIndicator.setVisibility(View.VISIBLE);
            btnSendCode.setEnabled(true);
            btnBackLogin.setEnabled(true);
        }
    }

    private void showError(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(Color.RED)
                .setTextColor(Color.WHITE)
                .setAction("THỬ LẠI", v -> {
                    tilEmail.setError(null);
                    etEmail.requestFocus();
                })
                .setActionTextColor(Color.WHITE)
                .show();
    }

    private void showSuccess(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(Color.GREEN)
                .setTextColor(Color.WHITE)
                .show();
    }

    private void animateButtonClick(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1f);

        scaleX.setDuration(200);
        scaleY.setDuration(200);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        scaleX.start();
        scaleY.start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Thêm animation khi back nếu cần
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}