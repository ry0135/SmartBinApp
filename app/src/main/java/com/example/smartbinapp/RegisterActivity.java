package com.example.smartbinapp;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.smartbinapp.model.Account;
import com.example.smartbinapp.model.Province;
import com.example.smartbinapp.model.Ward;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private ImageView ivRegisterBin;
    private LinearLayout headerSection;
    private CardView registerCard;
    private TextInputEditText etFullName, etEmail, etPhone, etPassword, etConfirmPassword, etAddressDetail;
    private TextInputLayout tilFullName, tilEmail, tilPhone, tilPassword, tilConfirmPassword, tilAddressDetail;
    private Spinner spinnerProvince, spinnerWard;
    private Button btnRegister;
    private TextView tvBackToLogin;

    private ApiService apiService;
    private Province selectedProvince;
    private Ward selectedWard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        initializeViews();

        // Init Retrofit
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // Load provinces
        loadProvinces();

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
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etAddressDetail = findViewById(R.id.et_address_detail);

        tilFullName = findViewById(R.id.til_full_name);
        tilEmail = findViewById(R.id.til_email);
        tilPhone = findViewById(R.id.til_phone);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        tilAddressDetail = findViewById(R.id.til_address_detail);

        spinnerProvince = findViewById(R.id.spinnerProvince);
        spinnerWard = findViewById(R.id.spinnerWard);

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

    private void loadProvinces() {
        apiService.getProvinces().enqueue(new Callback<List<Province>>() {
            @Override
            public void onResponse(Call<List<Province>> call, Response<List<Province>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Province> provinces = response.body();

                    ArrayAdapter<Province> adapter = new ArrayAdapter<>(
                            RegisterActivity.this,
                            android.R.layout.simple_spinner_item,
                            provinces
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerProvince.setAdapter(adapter);

                    spinnerProvince.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedProvince = provinces.get(position);
                            loadWards(selectedProvince.getProvinceId());
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                }
            }

            @Override
            public void onFailure(Call<List<Province>> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Lỗi tải Tỉnh/TP", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadWards(int provinceId) {
        apiService.getWards(provinceId).enqueue(new Callback<List<Ward>>() {
            @Override
            public void onResponse(Call<List<Ward>> call, Response<List<Ward>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Ward> wards = response.body();

                    ArrayAdapter<Ward> adapter = new ArrayAdapter<>(
                            RegisterActivity.this,
                            android.R.layout.simple_spinner_item,
                            wards
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerWard.setAdapter(adapter);

                    spinnerWard.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedWard = wards.get(position);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                }
            }

            @Override
            public void onFailure(Call<List<Ward>> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Lỗi tải Phường/Xã", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performRegistration() {
        clearErrors();

        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String addressDetail = etAddressDetail.getText().toString().trim();

        if (!validateInputs(fullName, email, phone, password, confirmPassword, addressDetail)) {
            return;
        }

        String fullAddress = (selectedWard != null ? selectedWard.getWardName() : "") + ", "
                + (selectedProvince != null ? selectedProvince.getProvinceName() : "") + ", "
                + addressDetail;

        // ⚡ Tạo đối tượng Account
        Account account = new Account();
        account.setFullName(fullName);
        account.setEmail(email);
        account.setPhone(phone);
        account.setWardId(selectedWard.getWardId());
        account.setPassword(password);
        account.setRole(4); // mặc định role = 3
        account.setStatus(0);

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
                    }, 1000);
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
                                   String password, String confirmPassword, String addressDetail) {
        boolean isValid = true;

        // 🧍‍♂️ Kiểm tra Họ và Tên
        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError("Vui lòng nhập họ và tên");
            isValid = false;
        }

        // 📧 Kiểm tra Email
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            isValid = false;
        }

        // 📱 Kiểm tra Số điện thoại
        if (TextUtils.isEmpty(phone) || !phone.matches("^0\\d{9,10}$")) {
            tilPhone.setError("Số điện thoại không hợp lệ (phải 10–11 số và bắt đầu bằng 0)");
            isValid = false;
        }

        // 🔐 Kiểm tra độ mạnh của Mật khẩu
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            isValid = false;
        } else if (password.length() < 8) {
            tilPassword.setError("Mật khẩu phải có ít nhất 8 ký tự");
            isValid = false;
        } else if (!password.matches(".*[A-Z].*")) {
            tilPassword.setError("Mật khẩu phải chứa ít nhất 1 chữ cái in hoa (A-Z)");
            isValid = false;
        } else if (!password.matches(".*[a-z].*")) {
            tilPassword.setError("Mật khẩu phải chứa ít nhất 1 chữ cái thường (a-z)");
            isValid = false;
        } else if (!password.matches(".*\\d.*")) {
            tilPassword.setError("Mật khẩu phải chứa ít nhất 1 chữ số (0-9)");
            isValid = false;
        } else if (!password.matches(".*[@#$%^&+=!._-].*")) {
            tilPassword.setError("Mật khẩu phải chứa ít nhất 1 ký tự đặc biệt (@#$%^&+=!._-)");
            isValid = false;
        }

        // ✅ Kiểm tra xác nhận mật khẩu
        if (TextUtils.isEmpty(confirmPassword) || !password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            isValid = false;
        }

        // 🏠 Kiểm tra địa chỉ chi tiết
        if (TextUtils.isEmpty(addressDetail)) {
            tilAddressDetail.setError("Vui lòng nhập địa chỉ chi tiết");
            isValid = false;
        }

        return isValid;
    }
    private void clearErrors() {
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        tilAddressDetail.setError(null);
    }

    private void animateButtonClick(View view) {
        ObjectAnimator scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f);
        scaleDown.setDuration(100);
        scaleDown.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f);
        scaleUp.setDuration(100);
        scaleUp.setStartDelay(100);
        scaleUp.setInterpolator(new AccelerateDecelerateInterpolator());

        scaleDown.start();
        scaleUp.start();
    }
}
