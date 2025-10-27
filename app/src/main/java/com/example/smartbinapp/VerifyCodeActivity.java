package com.example.smartbinapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartbinapp.model.Account;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyCodeActivity extends AppCompatActivity {

    private TextInputEditText etVerifyCode;
    private Button btnVerify;
    private TextView tvResendCode;
    private String email, mode;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_code);

        etVerifyCode = findViewById(R.id.et_verify_code);
        btnVerify = findViewById(R.id.btn_verify);
        tvResendCode = findViewById(R.id.tv_resend_code);

        email = getIntent().getStringExtra("EMAIL");
        mode = getIntent().getStringExtra("MODE");
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        btnVerify.setOnClickListener(v -> {
            String inputCode = etVerifyCode.getText() != null ? etVerifyCode.getText().toString().trim() : "";

            if (TextUtils.isEmpty(inputCode)) {
                Toast.makeText(this, "Vui lòng nhập mã xác minh!", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyCodeWithServer(email, inputCode);
        });

        tvResendCode.setOnClickListener(v -> {
            Toast.makeText(this, "Đang gửi lại mã xác minh...", Toast.LENGTH_SHORT).show();
            // TODO: Gọi API resend nếu có
        });
    }

    private void verifyCodeWithServer(String email, String code) {
        Account request = new Account(email, code);

        apiService.verifyCode(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(VerifyCodeActivity.this, "Xác thực thành công!", Toast.LENGTH_SHORT).show();

                    if ("forgot".equals(mode)) {
                        Intent intent = new Intent(VerifyCodeActivity.this, HomeActivity.class);
                        intent.putExtra("EMAIL", email);
                        startActivity(intent);
                    } else {
                        startActivity(new Intent(VerifyCodeActivity.this, LoginActivity.class));
                    }
                    finish();
                } else {
                    Toast.makeText(VerifyCodeActivity.this, "Mã không hợp lệ!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(VerifyCodeActivity.this, "Lỗi kết nối!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
