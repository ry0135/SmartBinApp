package com.example.smartbinapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartbinapp.model.Account;
import com.example.smartbinapp.model.UpdateProfileResponse;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPhone, etAddress;
    private Button btnSaveProfile;
    private ImageView ivAvatarEdit;

    private ApiService apiService;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etFullName = findViewById(R.id.etFullName);
//        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        ivAvatarEdit = findViewById(R.id.ivAvatarEdit);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // Lấy dữ liệu từ SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        userId = prefs.getString("userId", null);

        etFullName.setText(prefs.getString("userName", ""));
//        etEmail.setText(prefs.getString("email", ""));
        etPhone.setText(prefs.getString("phone", ""));
        etAddress.setText(prefs.getString("address", ""));

        btnSaveProfile.setOnClickListener(v -> updateProfile());

        ivAvatarEdit.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng đổi ảnh sẽ làm sau", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateProfile() {
        if (userId == null) {
            Toast.makeText(this, "Không xác định được tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = etFullName.getText().toString().trim();
//        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        Account account = new Account();
        account.setAccountId(Integer.parseInt(userId));
        account.setFullName(fullName);
//        account.setEmail(email);
        account.setPhone(phone);
        account.setAddress(address);

        apiService.updateAccount(userId, account).enqueue(new Callback<UpdateProfileResponse>() {
            @Override
            public void onResponse(Call<UpdateProfileResponse> call, Response<UpdateProfileResponse> response) {
                // Log để debug
                System.out.println("Response code: " + response.code());
                System.out.println("Response body: " + response.body());
                
                if (response.isSuccessful() && response.body() != null) {
                    UpdateProfileResponse updateResponse = response.body();
                    
                    if (updateResponse.isSuccess()) {
                        // Lưu lại session mới
                        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("userName", fullName);
                        editor.putString("phone", phone);
                        editor.putString("address", address);
                        editor.apply();

                        Toast.makeText(EditProfileActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String errorMsg = updateResponse.getMessage() != null ? 
                            updateResponse.getMessage() : "Không thể cập nhật thông tin";
                        Toast.makeText(EditProfileActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMessage = "Lỗi cập nhật (Code: " + response.code() + ")";
                    if (response.code() == 401) {
                        errorMessage = "Không có quyền cập nhật";
                    } else if (response.code() == 500) {
                        errorMessage = "Lỗi máy chủ, vui lòng thử lại sau";
                    } else if (response.code() == 404) {
                        errorMessage = "Không tìm thấy tài khoản";
                    }
                    Toast.makeText(EditProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UpdateProfileResponse> call, Throwable t) {
                System.out.println("Update error: " + t.getMessage());
                Toast.makeText(EditProfileActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}