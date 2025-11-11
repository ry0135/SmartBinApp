package com.example.smartbinapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText etNewPassword, etConfirmPassword;
    private TextInputLayout tilNewPassword, tilConfirmPassword;
    private Button btnReset;
    private ApiService apiService;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // 🧩 Ánh xạ view
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        tilNewPassword = findViewById(R.id.til_new_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        btnReset = findViewById(R.id.btn_reset_password);

        email = getIntent().getStringExtra("EMAIL");
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        btnReset.setOnClickListener(v -> {
            clearErrors();
            String newPass = etNewPassword.getText().toString().trim();
            String confirm = etConfirmPassword.getText().toString().trim();

            // ✅ Kiểm tra đầu vào
            if (!validatePassword(newPass, confirm)) {
                return;
            }

            // ✅ Gọi API đặt lại mật khẩu
            apiService.resetPassword(email, newPass).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(ResetPasswordActivity.this, "Đặt lại mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(ResetPasswordActivity.this, "Thất bại! Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(ResetPasswordActivity.this, "Lỗi kết nối!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * 🧩 Validate độ mạnh mật khẩu và xác nhận khớp
     */
    private boolean validatePassword(String password, String confirmPassword) {
        boolean isValid = true;

        if (TextUtils.isEmpty(password)) {
            tilNewPassword.setError("Vui lòng nhập mật khẩu mới");
            isValid = false;
        } else if (password.length() < 8) {
            tilNewPassword.setError("Mật khẩu phải có ít nhất 8 ký tự");
            isValid = false;
        } else if (!password.matches(".*[A-Z].*")) {
            tilNewPassword.setError("Mật khẩu phải chứa ít nhất 1 chữ cái in hoa (A-Z)");
            isValid = false;
        } else if (!password.matches(".*[a-z].*")) {
            tilNewPassword.setError("Mật khẩu phải chứa ít nhất 1 chữ cái thường (a-z)");
            isValid = false;
        } else if (!password.matches(".*\\d.*")) {
            tilNewPassword.setError("Mật khẩu phải chứa ít nhất 1 chữ số (0-9)");
            isValid = false;
        } else if (!password.matches(".*[@#$%^&+=!._-].*")) {
            tilNewPassword.setError("Mật khẩu phải chứa ít nhất 1 ký tự đặc biệt (@#$%^&+=!._-)");
            isValid = false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Vui lòng nhập xác nhận mật khẩu");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            isValid = false;
        }

        return isValid;
    }

    /**
     * 🧽 Xóa lỗi trước khi nhập lại
     */
    private void clearErrors() {
        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);
    }
}
