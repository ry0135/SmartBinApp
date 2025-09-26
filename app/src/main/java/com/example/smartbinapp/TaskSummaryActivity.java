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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbinapp.adapter.TaskSummaryAdapter;
import com.example.smartbinapp.model.TaskSummary;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskSummaryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TaskSummaryAdapter adapter;
    private int workerId;
    private ImageView ivMenu;

    private LinearLayout btnHome,btnReport, btnShowTask, btnAccount;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_summary);

        // Lấy workerId từ SharedPreferences
        // Lấy workerId từ SharedPreferences "UserSession"
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String savedUserId = prefs.getString("userId", "0");

// Ép kiểu sang int (nếu có giá trị)
        if (savedUserId != null) {
            workerId = Integer.parseInt(savedUserId);
        } else {
            workerId = 0; // fallback
        }
        recyclerView = findViewById(R.id.recyclerTaskSummary);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadTaskSummaries();
        initializeViews();
        setupClickListeners();
    }

    private void loadTaskSummaries() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.getTaskSummaries(workerId).enqueue(new Callback<List<TaskSummary>>() {
            @Override
            public void onResponse(Call<List<TaskSummary>> call, Response<List<TaskSummary>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter = new TaskSummaryAdapter(response.body(), summary -> {
                        Intent intent = new Intent(TaskSummaryActivity.this, TaskDetailActivity.class);
                        intent.putExtra("batchId", summary.getBatchId());
                        intent.putExtra("workerId", workerId);
                        startActivity(intent);
                    });
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(TaskSummaryActivity.this, "Không tải được dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<TaskSummary>> call, Throwable t) {
                Toast.makeText(TaskSummaryActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void initializeViews() {
        ivMenu = findViewById(R.id.iv_menu);
        btnHome = findViewById(R.id.btn_home);
        btnShowTask = findViewById(R.id.btn_showtask);
        btnAccount = findViewById(R.id.btn_account);
        btnReport = findViewById(R.id.btn_report);
    }

    private void setupClickListeners() {
//        ivMenu.setOnClickListener(v -> {
//            animateButtonClick(v);
//            Toast.makeText(this, "Menu", Toast.LENGTH_SHORT).show();
//        });

        btnHome.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, true);
            setActiveTab(btnReport, false);
            setActiveTab(btnShowTask, false);
            setActiveTab(btnAccount, false);
            Intent intent = new Intent(TaskSummaryActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        btnReport.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, false);
            setActiveTab(btnReport, true);
            setActiveTab(btnShowTask, false);
            setActiveTab(btnAccount, false);
            Intent intent = new Intent(TaskSummaryActivity.this, TaskSummaryActivity.class);
            startActivity(intent);
            finish();
        });

        btnShowTask.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, false);
            setActiveTab(btnShowTask, true);
            setActiveTab(btnReport, false);
            setActiveTab(btnAccount, false);
            Intent intent = new Intent(TaskSummaryActivity.this, TaskSummaryActivity.class);
            startActivity(intent);
            finish();
        });
        btnAccount.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, false);
            setActiveTab(btnReport, false);
            setActiveTab(btnShowTask, false);
            setActiveTab(btnAccount, true);
            Intent intent = new Intent(TaskSummaryActivity.this, ProfileActivity.class);
            startActivity(intent);
            finish();
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
