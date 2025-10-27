
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
import com.example.smartbinapp.model.LoginRequest;
import com.example.smartbinapp.model.LoginResponse;
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

        // Check session: Nếu đã đăng nhập trước đó thì bỏ qua màn hình Login
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String savedUserId = prefs.getString("userId", null);
        if (savedUserId != null) {
            int savedRole = prefs.getInt("role", 0);
            Intent intent;
            if (savedRole == 4) {
                intent = new Intent(LoginActivity.this, HomeActivityCitizen.class);
            } else {
                intent = new Intent(LoginActivity.this, HomeActivity.class);
            }
            intent.putExtra("firstname", prefs.getString("userName", ""));
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        // Khởi tạo View
        initializeViews();

        // Khởi tạo Retrofit
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // Điền lại username/email gần nhất nếu có
        String lastUsername = prefs.getString("lastUsername", null);
        if (lastUsername != null && etUsername != null) {
            etUsername.setText(lastUsername);
        }

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
        ObjectAnimator headerAnimator = ObjectAnimator.ofFloat(headerSection, "alpha", 0f, 1f);
        headerAnimator.setDuration(1000);
        headerAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        headerAnimator.start();

        ObjectAnimator loginCardAnimator = ObjectAnimator.ofFloat(loginCard, "alpha", 0f, 1f);
        loginCardAnimator.setDuration(800);
        loginCardAnimator.setStartDelay(300);
        loginCardAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        loginCardAnimator.start();

        ObjectAnimator quickLoginAnimator = ObjectAnimator.ofFloat(quickLoginSection, "alpha", 0f, 1f);
        quickLoginAnimator.setDuration(800);
        quickLoginAnimator.setStartDelay(600);
        quickLoginAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        quickLoginAnimator.start();

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

        // Clear previous errors
        tilUsername.setError(null);
        tilPassword.setError(null);

        if (username.isEmpty()) {
            tilUsername.setError("Vui lòng nhập tên đăng nhập");
            etUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("lastUsername", username);
        editor.apply();

        LoginRequest loginRequest = new LoginRequest(username, password);

        // Sử dụng raw endpoint để parse JSON bọc
        apiService.loginRaw(loginRequest).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                // Log để debug
                System.out.println("Response code: " + response.code());
                System.out.println("Request data: email=" + username + ", password=" + password);
                System.out.println("LoginRequest object: " + loginRequest.toString());
                
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        System.out.println("Raw response body: " + responseBody);
                        parseLoginResponseRaw(responseBody);
                    } catch (Exception e) {
                        System.out.println("Error reading response body: " + e.getMessage());
                        Toast.makeText(LoginActivity.this, "Lỗi đọc dữ liệu phản hồi", Toast.LENGTH_SHORT).show();
                    }

                    Account account = response.body();

                    // Lưu session
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("userId", String.valueOf(account.getAccountId()));
                    editor.putString("userName", account.getFullName());
                    editor.putString("email", account.getEmail());
                    editor.putLong("lastLoginTime", System.currentTimeMillis());
                    editor.apply();

                    // ✅ Lấy token FCM và gửi lên server
                    FirebaseMessaging.getInstance().getToken()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    String token = task.getResult();
                                    Log.d("FCM", "FCM Token: " + token);

                                    Map<String, String> body = new HashMap<>();
                                    body.put("token", token);

                                    apiService.updateFcmToken(account.getAccountId(), body)
                                            .enqueue(new Callback<ApiMessage>() {
                                                @Override
                                                public void onResponse(Call<ApiMessage> call,
                                                                       Response<ApiMessage> response) {
                                                    if (response.isSuccessful()) {
                                                        Log.d("FCM", "✅ Token saved to server");
                                                    } else {
                                                        Log.e("FCM", "❌ Error saving token: " + response.code());
                                                    }
                                                }

                                                @Override
                                                public void onFailure(Call<ApiMessage> call, Throwable t) {
                                                    Log.e("FCM", "❌ Failed to save token: " + t.getMessage());
                                                }
                                            });
                                }
                            });

                    // Chuyển sang HomeActivity
                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    intent.putExtra("firstname", account.getFullName());
                    startActivity(intent);
                    finish();

                } else {
                    showLoginError(response.code());
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                System.out.println("Login error: " + t.getMessage());
                Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void parseLoginResponseRaw(String responseString) {
        try {
            System.out.println("Parsing raw login response: " + responseString);
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.google.gson.JsonObject root = gson.fromJson(responseString, com.google.gson.JsonObject.class);
            
            if (root != null && root.has("data") && root.get("data").isJsonObject()) {
                com.google.gson.JsonObject data = root.getAsJsonObject("data");
                if (data.has("account") && data.get("account").isJsonObject()) {
                    com.google.gson.JsonObject account = data.getAsJsonObject("account");
                    
                    // Extract account info
                    int accountId = account.has("accountId") ? account.get("accountId").getAsInt() : 0;
                    String fullName = account.has("fullName") ? account.get("fullName").getAsString() : "";
                    String email = account.has("email") ? account.get("email").getAsString() : "";
                    int role = account.has("role") ? account.get("role").getAsInt() : 0;
                    
                    if (accountId > 0) {
                        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("userId", String.valueOf(accountId));
                        editor.putString("userName", fullName);
                        editor.putString("email", email);
                        editor.putInt("role", role);
                        editor.putLong("lastLoginTime", System.currentTimeMillis());
                        editor.apply();

                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, role == 4 ? HomeActivityCitizen.class : HomeActivity.class);
                        intent.putExtra("firstname", fullName);
                        startActivity(intent);
                        finish();
                        return;
                    }
                }
            }
            
            // Nếu không parse được, hiển thị lỗi
            showLoginError(0);
        } catch (Exception e) {
            System.out.println("Error parsing raw login response: " + e.getMessage());
            showLoginError(0);
        }
    }

    private void showLoginError(int responseCode) {
        String errorMessage = "Lỗi đăng nhập";
        if (responseCode == 401) {
            errorMessage = "Sai tên đăng nhập hoặc mật khẩu";
        } else if (responseCode == 500) {
            errorMessage = "Lỗi máy chủ, vui lòng thử lại sau";
        } else if (responseCode == 404) {
            errorMessage = "Không tìm thấy tài khoản";
        } else if (responseCode > 0) {
            errorMessage = "Lỗi đăng nhập (Code: " + responseCode + ")";
        }
        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
    }
}
