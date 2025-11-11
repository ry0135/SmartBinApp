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
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbinapp.adapter.NotificationAdapter;
import com.example.smartbinapp.model.Notification;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ImageView btnBack;
    private LinearLayout btnHome, btnShowTask, btnAccount, btnReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_list);

        initializeViews();
        setupClickListeners();
        startEntranceAnimations();
        loadNotifications();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_notifications);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btn_back);

        btnHome = findViewById(R.id.btn_home);
        btnShowTask = findViewById(R.id.btn_showtask);
        btnAccount = findViewById(R.id.btn_account);
        btnReport = findViewById(R.id.btn_report);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        int savedRole = prefs.getInt("role", 0);
        if (savedRole == 4) btnShowTask.setVisibility(View.GONE);
    }

    private void startEntranceAnimations() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(findViewById(R.id.header_layout),
                "translationY", -100f, 0f);
        animator.setDuration(800);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();

        ObjectAnimator bottomNavAnimator = ObjectAnimator.ofFloat(findViewById(R.id.bottom_navigation),
                "translationY", 100f, 0f);
        bottomNavAnimator.setDuration(800);
        bottomNavAnimator.setStartDelay(200);
        bottomNavAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        bottomNavAnimator.start();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnAccount.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        btnShowTask.setOnClickListener(v -> startActivity(new Intent(this, TaskSummaryActivity.class)));
        btnReport.setOnClickListener(v -> startActivity(new Intent(this, ReportsListActivity.class)));
    }

    private void loadNotifications() {
        progressBar.setVisibility(ProgressBar.VISIBLE);

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String accountId = prefs.getString("userId", "");

        apiService.getReceivedNotifications(accountId).enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(Call<List<Notification>> call, Response<List<Notification>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    recyclerView.setAdapter(new NotificationAdapter(NotificationListActivity.this, response.body()));
                }
            }

            @Override
            public void onFailure(Call<List<Notification>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e("NotificationList", "Lỗi API: " + t.getMessage());
            }
        });
    }
}
