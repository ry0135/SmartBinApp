package com.example.smartbinapp;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.smartbinapp.model.Account;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvEmail;
    private ImageView ivMenu;
    private LinearLayout btnHome, btnReport, btnShowTask, btnAccount;
    private ImageView ivAvatar;
    private LinearLayout itemEditProfile, itemChangePassword, itemNotification, itemHelp, itemLogout;

    private ApiService apiService;
    private SharedPreferences prefs;
    private Account currentAccount; // Dùng để lưu account lấy từ server
    private static final int ROLE_TO_HIDE_TASK = 4;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1️⃣ Ánh xạ view
        ivAvatar = findViewById(R.id.ivAvatar);
        tvUserName = findViewById(R.id.tvUserName);
        tvEmail = findViewById(R.id.tvEmail);

        itemEditProfile = findViewById(R.id.itemEditProfile);
        itemChangePassword = findViewById(R.id.itemChangePassword);
        itemNotification = findViewById(R.id.itemNotification);
        itemHelp = findViewById(R.id.itemHelp);
        itemLogout = findViewById(R.id.itemLogout);

        // 2️⃣ Khởi tạo SharedPreferences và API service
        prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // 3️⃣ Lấy userId từ session
        String savedUserId = prefs.getString("userId", null);
        if (savedUserId == null) {
            Toast.makeText(this, "Không tìm thấy userId trong session!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4️⃣ Gọi API lấy thông tin account
        fetchAccountFromApi(savedUserId);

        // 5️⃣ Các chức năng khác
        setupMenuClick();
        initializeBottomBar();
        setupBottomBarClickListeners();
    }

    private void fetchAccountFromApi(String userId) {
        apiService.getUserById(userId).enqueue(new Callback<Account>() {
            @Override
            public void onResponse(Call<Account> call, Response<Account> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentAccount = response.body();

                    tvUserName.setText(currentAccount.getFullName());
                    tvEmail.setText(currentAccount.getEmail());
                    handleTaskButtonVisibility(currentAccount.getRole());
                    // ✅ Nếu có avatarUrl → load bằng Glide
                    if (currentAccount.getAvatarUrl() != null && !currentAccount.getAvatarUrl().isEmpty()) {
                        Glide.with(ProfileActivity.this)
                                .load(currentAccount.getAvatarUrl())
                                .apply(new RequestOptions()
                                        .placeholder(R.drawable.defaul_avarta) // ảnh mặc định nếu chưa có
                                        .error(R.drawable.defaul_avarta)
                                        .circleCrop()
                                        .diskCacheStrategy(DiskCacheStrategy.ALL))
                                .into(ivAvatar);
                    } else {
                        // Nếu chưa có ảnh → hiện ảnh mặc định
                        ivAvatar.setImageResource(R.drawable.defaul_avarta);
                    }

                    // ✅ Lưu SharedPreferences
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("userId", String.valueOf(currentAccount.getAccountId()));
                    editor.putString("userName", currentAccount.getFullName());
                    editor.putString("email", currentAccount.getEmail());
                    editor.putString("phone", currentAccount.getPhone());
                    editor.putString("address", currentAccount.getAddressDetail());
                    if (currentAccount.getAvatarUrl() != null)
                        editor.putString("avatarUrl", currentAccount.getAvatarUrl());
                    editor.apply();

                } else {
                    Toast.makeText(ProfileActivity.this, "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Account> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void handleTaskButtonVisibility(int userRole) {
        if (userRole == ROLE_TO_HIDE_TASK) {
            // Nếu role là 4, ẩn nút
            btnShowTask.setVisibility(View.GONE);
        } else {
            // Ngược lại, hiện nút (hoặc giữ nguyên trạng thái default)
            btnShowTask.setVisibility(View.VISIBLE);
        }
    }

    private void setupMenuClick() {
        itemEditProfile.setOnClickListener(v -> {
            if (currentAccount == null) {
                Toast.makeText(this, "Đang tải thông tin, vui lòng chờ...", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            intent.putExtra("userId", currentAccount.getAccountId());
            intent.putExtra("userName", currentAccount.getFullName());
            intent.putExtra("email", currentAccount.getEmail());
            intent.putExtra("phone", currentAccount.getPhone());
            intent.putExtra("address", currentAccount.getAddressDetail());
            startActivity(intent);
        });

        itemChangePassword.setOnClickListener(v ->
        {
            Intent intent = new Intent(this, ChangePasswordActivity.class);
            startActivity(intent);
        });
        itemNotification.setOnClickListener(v ->
                Toast.makeText(this, "Mở cài đặt thông báo", Toast.LENGTH_SHORT).show());

        itemHelp.setOnClickListener(v ->
                Toast.makeText(this, "Mở trợ giúp & hỗ trợ", Toast.LENGTH_SHORT).show());

        itemLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void initializeBottomBar() {
        ivMenu = findViewById(R.id.iv_menu);
        btnHome = findViewById(R.id.btn_home);
        btnShowTask = findViewById(R.id.btn_showtask);
        btnAccount = findViewById(R.id.btn_account);
        btnReport = findViewById(R.id.btn_report);
    }

    private void setupBottomBarClickListeners() {
        ivMenu.setOnClickListener(v -> {
            animateButtonClick(v);
            Toast.makeText(this, "Menu", Toast.LENGTH_SHORT).show();
        });

        btnHome.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, true);
            startActivity(new Intent(ProfileActivity.this, HomeActivity.class));
            finish();
        });

        btnReport.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnReport, true);
            startActivity(new Intent(ProfileActivity.this, ReportsListActivity.class));
            finish();
        });

        btnShowTask.setOnClickListener(v -> {
            // Chỉ xử lý khi nút này HIỆN (vì có thể nó đã bị ẩn)
            if (btnShowTask.getVisibility() == View.VISIBLE) {
                animateButtonClick(v);
                setActiveTab(btnShowTask, true);
                startActivity(new Intent(ProfileActivity.this, TaskSummaryActivity.class));
                finish();
            }
        });

        btnAccount.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnAccount, true);
            // Đang ở màn này nên không cần mở lại
        });
    }

    private void setActiveTab(LinearLayout tab, boolean isActive) {
        ImageView icon = (ImageView) tab.getChildAt(0);
        TextView text = (TextView) tab.getChildAt(1);

        int activeColor = getResources().getColor(android.R.color.holo_green_dark);
        int inactiveColor = getResources().getColor(android.R.color.darker_gray);

        icon.setColorFilter(isActive ? activeColor : inactiveColor);
        text.setTextColor(isActive ? activeColor : inactiveColor);
    }

    private void animateButtonClick(View view) {
        ObjectAnimator scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f);
        scaleDown.setDuration(100);
        scaleDown.start();

        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f);
        scaleUp.setDuration(100);
        scaleUp.setStartDelay(100);
        scaleUp.start();
    }
}
