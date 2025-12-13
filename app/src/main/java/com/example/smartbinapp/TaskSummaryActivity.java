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

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskSummaryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TaskSummaryAdapter adapter;
    private int workerId;
    private LinearLayout btnHome, btnReport, btnShowTask, btnAccount;
    private LinearLayout tvEmptyState;

    // FILTER UI
    private TextView filterAll, filterDoing, filterCompleted, filterCancelled;

    // Data lists
    private List<TaskSummary> fullList = new ArrayList<>();
    private List<TaskSummary> filteredList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_summary);

        // Lấy workerId từ SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String savedUserId = prefs.getString("userId", "0");
        workerId = Integer.parseInt(savedUserId);

        initializeViews();
        setupClickListeners();
        setupFilterListeners();
        loadTaskSummaries();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerTaskSummary);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnHome = findViewById(R.id.btn_home);
        btnReport = findViewById(R.id.btn_report);
        btnShowTask = findViewById(R.id.btn_showtask);
        btnAccount = findViewById(R.id.btn_account);

        // FILTER BUTTONS
        filterAll = findViewById(R.id.filterAll);
        filterDoing = findViewById(R.id.filterDoing);
        filterCompleted = findViewById(R.id.filterCompleted);
        filterCancelled = findViewById(R.id.filterCancelled);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set active tab
        setActiveTab(btnShowTask, true);
    }

    private void loadTaskSummaries() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        showLoading(true);

        apiService.getTaskSummaries(workerId).enqueue(new Callback<List<TaskSummary>>() {
            @Override
            public void onResponse(Call<List<TaskSummary>> call, Response<List<TaskSummary>> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    fullList = response.body();  // lưu danh sách gốc

                    applyFilter("ALL"); // lọc mặc định

                } else {
                    showEmptyState(true);
                    Toast.makeText(TaskSummaryActivity.this, "Không tải được dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<TaskSummary>> call, Throwable t) {
                showLoading(false);
                showEmptyState(true);
                Toast.makeText(TaskSummaryActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openTaskDetail(TaskSummary summary) {
        Intent intent = new Intent(TaskSummaryActivity.this, TaskDetailActivity.class);
        intent.putExtra("batchId", summary.getBatchId());
        intent.putExtra("workerId", workerId);
        intent.putExtra("taskName", summary.getNote());
        intent.putExtra("priority", summary.getMinPriority());
        intent.putExtra("status", summary.getStatus());
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        if (show) {
            recyclerView.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(boolean show) {
        if (show) {
            recyclerView.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    // ----------------------------
    // FILTER FUNCTION
    // ----------------------------
    private void setupFilterListeners() {
        filterAll.setOnClickListener(v -> applyFilter("ALL"));
        filterDoing.setOnClickListener(v -> applyFilter("DOING"));
        filterCompleted.setOnClickListener(v -> applyFilter("COMPLETED"));
        filterCancelled.setOnClickListener(v -> applyFilter("CANCELLED"));

    }

    private void applyFilter(String type) {
        filteredList.clear();

        switch (type) {
            case "DOING":
                for (TaskSummary t : fullList)
                    if ("DOING".equalsIgnoreCase(t.getStatus()))
                        filteredList.add(t);
                break;

            case "COMPLETED":
                for (TaskSummary t : fullList)
                    if ("COMPLETED".equalsIgnoreCase(t.getStatus()))
                        filteredList.add(t);
                break;
            case "CANCELLED":
                for (TaskSummary t : fullList)
                    if ("CANCELLED".equalsIgnoreCase(t.getStatus()))
                        filteredList.add(t);
                break;

            default:
                filteredList.addAll(fullList);
        }

        updateFilterUI(type);

        adapter = new TaskSummaryAdapter(filteredList, summary -> openTaskDetail(summary));
        recyclerView.setAdapter(adapter);

        showEmptyState(filteredList.isEmpty());
    }

    private void updateFilterUI(String active) {

        filterAll.setBackgroundResource(active.equals("ALL") ?
                R.drawable.filter_active : R.drawable.filter_inactive);
        filterDoing.setBackgroundResource(active.equals("DOING") ?
                R.drawable.filter_active : R.drawable.filter_inactive);
        filterCompleted.setBackgroundResource(active.equals("COMPLETED") ?
                R.drawable.filter_active : R.drawable.filter_inactive);
        filterCancelled.setBackgroundResource(active.equals("CANCELLED") ?
                R.drawable.filter_active : R.drawable.filter_inactive);

        filterAll.setTextColor(active.equals("ALL") ? 0xFFFFFFFF : 0xFF777777);
        filterDoing.setTextColor(active.equals("DOING") ? 0xFFFFFFFF : 0xFF777777);
        filterCompleted.setTextColor(active.equals("COMPLETED") ? 0xFFFFFFFF : 0xFF777777);
        filterCancelled.setTextColor(active.equals("CANCELLED") ? 0xFFFFFFFF : 0xFF777777);

    }

    // ----------------------------
    // BOTTOM NAVIGATION
    // ----------------------------
    private void setupClickListeners() {
        btnHome.setOnClickListener(v -> {
            animateButtonClick(v);
            navigateToActivity(HomeActivity.class);
        });

        btnReport.setOnClickListener(v -> {
            animateButtonClick(v);
            navigateToActivity(ReportsListActivity.class);
        });

        btnShowTask.setOnClickListener(v -> {
            animateButtonClick(v);
            loadTaskSummaries();
        });

        btnAccount.setOnClickListener(v -> {
            animateButtonClick(v);
            navigateToActivity(ProfileActivity.class);
        });
    }

    private void navigateToActivity(Class<?> cls) {
        Intent intent = new Intent(TaskSummaryActivity.this, cls);
        startActivity(intent);
        finish();
    }

    private void setActiveTab(LinearLayout tab, boolean isActive) {
        ImageView icon = (ImageView) tab.getChildAt(0);
        TextView text = (TextView) tab.getChildAt(1);

        int activeColor = getResources().getColor(R.color.green_active);
        int inactiveColor = getResources().getColor(R.color.gray_inactive);

        icon.setColorFilter(isActive ? activeColor : inactiveColor);
        text.setTextColor(isActive ? activeColor : inactiveColor);
    }

    private void animateButtonClick(View view) {
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f);
        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);

        scaleDownX.start();
        scaleDownY.start();

        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f);
        scaleUpX.setDuration(100);
        scaleUpY.setDuration(100);
        scaleUpX.setStartDelay(100);
        scaleUpY.setStartDelay(100);

        scaleUpX.start();
        scaleUpY.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTaskSummaries();
    }
}
