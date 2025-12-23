package com.example.smartbinapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    private static final String TAG = "ChangePassword";

    private ImageView btnBack;
    private TextInputLayout tilCurrentPassword, tilNewPassword, tilConfirmPassword;
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private Button btnChangePassword, btnCancel;
    private ProgressBar progressBar;

    private ApiService apiService;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Initialize API service
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        userId = prefs.getString("userId", null);

        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);

        tilCurrentPassword = findViewById(R.id.tilCurrentPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnCancel = findViewById(R.id.btnCancel);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnCancel.setOnClickListener(v -> finish());

        btnChangePassword.setOnClickListener(v -> validateAndChangePassword());
    }

    private void validateAndChangePassword() {
        // Reset errors
        tilCurrentPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);

        // Get input values
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        boolean isValid = true;

        // Validate current password
        if (TextUtils.isEmpty(currentPassword)) {
            tilCurrentPassword.setError("Vui lòng nhập mật khẩu hiện tại");
            isValid = false;
        }

        // Validate new password
        if (TextUtils.isEmpty(newPassword)) {
            tilNewPassword.setError("Vui lòng nhập mật khẩu mới");
            isValid = false;
        } else if (!isPasswordValid(newPassword)) {
            tilNewPassword.setError("Mật khẩu không đáp ứng yêu cầu");
            isValid = false;
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Vui lòng xác nhận mật khẩu mới");
            isValid = false;
        } else if (!newPassword.equals(confirmPassword)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            isValid = false;
        }

        // Check if new password same as current
        if (isValid && currentPassword.equals(newPassword)) {
            tilNewPassword.setError("Mật khẩu mới phải khác mật khẩu hiện tại");
            isValid = false;
        }

        if (isValid) {
            changePassword(currentPassword, newPassword);
        }
    }

    private boolean isPasswordValid(String password) {
        // Tối thiểu 8 ký tự
        if (password.length() < 8) {
            return false;
        }

        // Có ít nhất 1 chữ hoa
        if (!password.matches(".*[A-Z].*")) {
            return false;
        }

        // Có ít nhất 1 chữ thường
        if (!password.matches(".*[a-z].*")) {
            return false;
        }

        // Có ít nhất 1 số
        if (!password.matches(".*\\d.*")) {
            return false;
        }

        return true;
    }

    private void changePassword(String currentPassword, String newPassword) {
        showLoading(true);

        // Tạo JSON body
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("accountId", Integer.parseInt(userId));
            jsonBody.put("oldPassword", currentPassword);
            jsonBody.put("newPassword", newPassword);
        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON: " + e.getMessage());
            showLoading(false);
            Toast.makeText(this, "Lỗi tạo yêu cầu", Toast.LENGTH_SHORT).show();
            return;
        }

        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/json"),
                jsonBody.toString()
        );

        apiService.changePassword(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                showLoading(false);

                if (response.isSuccessful()) {
                    try {
                        String responseString = response.body().string();
                        Log.d(TAG, "Response: " + responseString);

                        JSONObject jsonResponse = new JSONObject(responseString);

                        // Kiểm tra code hoặc success
                        boolean success = false;
                        if (jsonResponse.has("code")) {
                            success = jsonResponse.getInt("code") == 200;
                        } else if (jsonResponse.has("success")) {
                            success = jsonResponse.getBoolean("success");
                        }

                        if (success) {
                            Toast.makeText(ChangePasswordActivity.this,
                                    "Đổi mật khẩu thành công!",
                                    Toast.LENGTH_SHORT).show();

                            // Clear fields
                            etCurrentPassword.setText("");
                            etNewPassword.setText("");
                            etConfirmPassword.setText("");

                            // Đóng activity sau 1 giây
                            etCurrentPassword.postDelayed(() -> finish(), 1000);
                        } else {
                            String message = "Đổi mật khẩu thất bại";
                            if (jsonResponse.has("message")) {
                                message = jsonResponse.getString("message");
                            }
                            Toast.makeText(ChangePasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Parse error: " + e.getMessage());
                        Toast.makeText(ChangePasswordActivity.this,
                                "Đổi mật khẩu thành công!",
                                Toast.LENGTH_SHORT).show();
                        etCurrentPassword.postDelayed(() -> finish(), 1000);
                    }
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e(TAG, "Error response: " + errorBody);

                        JSONObject errorJson = new JSONObject(errorBody);
                        String errorMessage = "Đổi mật khẩu thất bại";

                        if (errorJson.has("message")) {
                            errorMessage = errorJson.getString("message");
                        }

                        // Kiểm tra lỗi mật khẩu hiện tại sai
                        if (errorMessage.toLowerCase().contains("current password") ||
                                errorMessage.toLowerCase().contains("mật khẩu hiện tại")) {
                            tilCurrentPassword.setError("Mật khẩu hiện tại không đúng");
                        } else {
                            Toast.makeText(ChangePasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(ChangePasswordActivity.this,
                                "Mật khẩu hiện tại không đúng",
                                Toast.LENGTH_SHORT).show();
                        tilCurrentPassword.setError("Mật khẩu hiện tại không đúng");
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(ChangePasswordActivity.this,
                        "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnChangePassword.setEnabled(!show);
        btnCancel.setEnabled(!show);

        etCurrentPassword.setEnabled(!show);
        etNewPassword.setEnabled(!show);
        etConfirmPassword.setEnabled(!show);
    }
}