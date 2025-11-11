package com.example.smartbinapp;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.smartbinapp.model.Account;
import com.example.smartbinapp.model.ApiMessage;
import com.example.smartbinapp.model.LoginRequest;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ImageView ivLoginBin;
    private LinearLayout headerSection;
    private CardView loginCard;
    private TextInputEditText etUsername, etPassword;
    private TextInputLayout tilUsername, tilPassword;

    private ApiService apiService;
    private static final int RC_GOOGLE_SIGN_IN = 100;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🟢 Kiểm tra session cũ
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String savedUserId = prefs.getString("userId", null);

        if (savedUserId != null) {

            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            intent.putExtra("firstname", prefs.getString("userName", ""));
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        initializeViews();
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        startEntranceAnimations();
        setupButtonListeners();

        // 🟢 Cấu hình Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.btn_google_custom).setOnClickListener(v -> {
            animateButtonClick(v);
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
        });
    }

    private void initializeViews() {
        ivLoginBin = findViewById(R.id.iv_login_bin);
        headerSection = findViewById(R.id.header_section);
        loginCard = findViewById(R.id.login_card);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        tilUsername = findViewById(R.id.til_username);
        tilPassword = findViewById(R.id.til_password);
    }

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

        findViewById(R.id.tv_forgot_password).setOnClickListener(v -> {
            animateButtonClick(v);
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });
    }

    private void performLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        tilUsername.setError(null);
        tilPassword.setError(null);

        if (username.isEmpty()) {
            tilUsername.setError("Vui lòng nhập email");
            etUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        LoginRequest loginRequest = new LoginRequest(username, password);

        apiService.loginRaw1(loginRequest).enqueue(new Callback<Account>() {
            @Override
            public void onResponse(Call<Account> call, Response<Account> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Account acc = response.body();

                    // ✅ Check null an toàn
                    Integer accountId = acc.getAccountId();
                    Integer role = acc.getRole();

                    if (accountId == null) {
                        Toast.makeText(LoginActivity.this, "Lỗi: Không lấy được ID tài khoản", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveSession(accountId, acc.getFullName(), acc.getEmail(), role != null ? role : 0);

                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();

                    Intent nextIntent = (role != null && role == 4)
                            ? new Intent(LoginActivity.this, HomeActivity.class)
                            : new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(nextIntent);
                    finish();
                } else {
                    showLoginError(response.code());
                }
            }

            @Override
            public void onFailure(Call<Account> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    String idToken = account.getIdToken();
                    String email = account.getEmail();
                    String fullName = account.getDisplayName();

                    Map<String, String> body = new HashMap<>();
                    body.put("idToken", idToken);

                    apiService.loginWithGoogle(body).enqueue(new Callback<ApiMessage>() {
                        @Override
                        public void onResponse(Call<ApiMessage> call, Response<ApiMessage> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                ApiMessage msg = response.body();
                                int accountId = msg.getUserId();
                                int role = msg.getRole();

                                saveSession(accountId, fullName, email, role);
                                Toast.makeText(LoginActivity.this, msg.getMessage(), Toast.LENGTH_SHORT).show();

                                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                finish();
                            }
                        }
                        @Override
                        public void onFailure(Call<ApiMessage> call, Throwable t) {
                            Toast.makeText(LoginActivity.this, "Kết nối thất bại: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In thất bại", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ✅ Đã fix an toàn cho role, null check
    private void saveSession(int accountId, String fullName, String email, int role) {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("userId", String.valueOf(accountId));
        editor.putString("userName", fullName);
        editor.putString("email", email);
        editor.putInt("role", role);
        editor.putLong("lastLoginTime", System.currentTimeMillis());
        editor.apply();

        // 🟢 Lưu FCM token
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                if (token != null && !token.isEmpty()) {
                    Map<String, String> body = new HashMap<>();
                    body.put("token", token);
                    apiService.updateFcmToken(accountId, body).enqueue(new Callback<com.example.smartbinapp.model.ApiMessage>() {
                        @Override
                        public void onResponse(Call<com.example.smartbinapp.model.ApiMessage> call, Response<com.example.smartbinapp.model.ApiMessage> response) {
                            Log.d("FCM", "✅ Token saved to server");
                        }

                        @Override
                        public void onFailure(Call<com.example.smartbinapp.model.ApiMessage> call, Throwable t) {
                            Log.e("FCM", "❌ Failed to save token: " + t.getMessage());
                        }
                    });
                }
            }
        });
    }

    private void showLoginError(int responseCode) {
        String errorMessage = "Lỗi đăng nhập";
        if (responseCode == 401) errorMessage = "Sai tên đăng nhập hoặc mật khẩu";
        else if (responseCode == 500) errorMessage = "Lỗi máy chủ, vui lòng thử lại sau";
        else if (responseCode == 404) errorMessage = "Không tìm thấy tài khoản";
        else if (responseCode == 403) errorMessage = "Tài khoản đã bị khóa";
        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
    }
}