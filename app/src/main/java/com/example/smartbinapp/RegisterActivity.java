package com.example.smartbinapp;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.smartbinapp.model.Account;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private ImageView ivRegisterBin;
    private LinearLayout headerSection;
    private CardView registerCard;
    private TextInputEditText etFullName, etUsername, etEmail, etPhone, etAddress, etPassword, etConfirmPassword;
    private TextInputLayout tilFullName, tilUsername, tilEmail, tilPhone, tilAddress, tilPassword, tilConfirmPassword;
    private Button btnRegister;
    private TextView tvBackToLogin;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        initializeViews();
        
        // Start entrance animations
        startEntranceAnimations();
        
        // Set up button click listeners
        setupButtonListeners();
    }

    private void initializeViews() {
        ivRegisterBin = findViewById(R.id.iv_register_bin);
        headerSection = findViewById(R.id.header_section);
        registerCard = findViewById(R.id.register_card);
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etAddress = findViewById(R.id.et_address);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        tilFullName = findViewById(R.id.til_full_name);
        tilEmail = findViewById(R.id.til_email);
        tilPhone = findViewById(R.id.til_phone);
        tilAddress = findViewById(R.id.til_address);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvBackToLogin = findViewById(R.id.tv_back_to_login);
    }

    private void startEntranceAnimations() {
        // Animate header section
        ObjectAnimator headerAnimator = ObjectAnimator.ofFloat(headerSection, "alpha", 0f, 1f);
        headerAnimator.setDuration(1000);
        headerAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        headerAnimator.start();

        // Animate register card with delay
        ObjectAnimator registerCardAnimator = ObjectAnimator.ofFloat(registerCard, "alpha", 0f, 1f);
        registerCardAnimator.setDuration(800);
        registerCardAnimator.setStartDelay(300);
        registerCardAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        registerCardAnimator.start();

        // Animate smart bin icon with rotation
        ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(ivRegisterBin, "rotationY", 0f, 360f);
        rotationAnimator.setDuration(1500);
        rotationAnimator.setStartDelay(500);
        rotationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        rotationAnimator.start();
    }

    private void setupButtonListeners() {
        btnRegister.setOnClickListener(v -> {
            animateButtonClick(v);
            performRegistration();
        });

        tvBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void performRegistration() {
        clearErrors();

        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (!validateInputs(fullName, email, phone, address, password, confirmPassword)) {
            return;
        }


        // ⚡ Tạo đối tượng Account (tùy constructor bạn đang có)
        Account account = new Account();
        account.setFullName(fullName);  // hoặc setFullName() tùy thuộc model
        account.setEmail(email);
        account.setPhone(phone);
        account.setAddress(address);
        account.setPassword(password);
        account.setRole(3); // ví dụ: role mặc định = 3
        account.setStatus(0);

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.register(account).enqueue(new Callback<Account>() {
            @Override
            public void onResponse(Call<Account> call, Response<Account> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Intent intent = new Intent(RegisterActivity.this, VerifyCodeActivity.class);
                        intent.putExtra("EMAIL", account.getEmail());
                        startActivity(intent);
                        finish();
                    }, 1000); // 1 giây
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e("REGISTER_ERROR", "Error body: " + errorBody);
                        Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(RegisterActivity.this, "Đăng ký thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Account> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateInputs(String fullName, String email, String phone,
                                 String address, String password, String confirmPassword) {
        boolean isValid = true;

        // Validate full name
        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError("Vui lòng nhập họ và tên");
            isValid = false;
        } else if (fullName.length() < 2) {
            tilFullName.setError("Họ và tên phải có ít nhất 2 ký tự");
            isValid = false;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Vui lòng nhập email");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            isValid = false;
        }

        // Validate phone
        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError("Vui lòng nhập số điện thoại");
            isValid = false;
        } else if (phone.length() < 10) {
            tilPhone.setError("Số điện thoại không hợp lệ");
            isValid = false;
        }

        // Validate address
        if (TextUtils.isEmpty(address)) {
            tilAddress.setError("Vui lòng nhập địa chỉ");
            isValid = false;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            isValid = false;
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Vui lòng xác nhận mật khẩu");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            isValid = false;
        }

        return isValid;
    }

    private void clearErrors() {
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilAddress.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
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




