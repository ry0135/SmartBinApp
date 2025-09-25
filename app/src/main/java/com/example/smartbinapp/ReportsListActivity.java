package com.example.smartbinapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.smartbinapp.adapter.ReportsAdapter;
import com.example.smartbinapp.model.Report;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportsListActivity extends AppCompatActivity implements ReportsAdapter.OnReportClickListener {

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvReports;
    private TextView tvEmpty;
    private ProgressBar progressBar;

    private ApiService apiService;
    private ReportsAdapter adapter;
    private List<Report> allReports = new ArrayList<>();
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports_list);

        // Initialize API service
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem báo cáo", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components
        initializeViews();
        setupToolbar();
        setupTabLayout();
        setupSwipeRefresh();
        setupRecyclerView();

        // Load reports
        loadReports();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvReports = findViewById(R.id.rvReports);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        // Set up home button click listener
        Button btnBackToHome = findViewById(R.id.btnBackToHome);
        btnBackToHome.setOnClickListener(v -> {
            Intent intent = new Intent(ReportsListActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterReports(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadReports);
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
    }

    private void setupRecyclerView() {
        adapter = new ReportsAdapter(this, this);
        rvReports.setAdapter(adapter);
    }

    private void loadReports() {
        showLoading(true);
        // Gọi raw để tự parse JSON bọc {status, message, data}
        apiService.getUserReportsRaw(userId).enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call, retrofit2.Response<okhttp3.ResponseBody> response) {
                try {
                    swipeRefreshLayout.setRefreshing(false);
                    showLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        android.util.Log.d("ReportsListActivity", "Raw reports body: " + body);
                        parseReportsResponse(body);
                    } else {
                        android.util.Log.w("ReportsListActivity", "Raw endpoint failed, trying typed endpoint");
                        loadReportsTyped();
                    }
                } catch (Exception e) {
                    android.util.Log.e("ReportsListActivity", "Parse raw failed: " + e.getMessage());
                    loadReportsTyped();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                showLoading(false);
                android.util.Log.e("ReportsListActivity", "Raw call failed: " + t.getMessage());
                loadReportsTyped();
            }
        });
    }

    private void loadReportsTyped() {
        showLoading(true);
        apiService.getUserReports(userId).enqueue(new Callback<List<Report>>() {
            @Override
            public void onResponse(Call<List<Report>> call, Response<List<Report>> response) {
                swipeRefreshLayout.setRefreshing(false);
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    allReports = response.body();
                    filterReports(tabLayout.getSelectedTabPosition());
                } else {
                    android.util.Log.w("ReportsListActivity", "Typed endpoint failed, trying old raw endpoint");
                    loadReportsOldRaw();
                }
            }

            @Override
            public void onFailure(Call<List<Report>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                showLoading(false);
                android.util.Log.e("ReportsListActivity", "Typed call failed: " + t.getMessage());
                loadReportsOldRaw();
            }
        });
    }

    private void loadReportsOldRaw() {
        showLoading(true);
        apiService.getUserReportsOldRaw(userId).enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call, retrofit2.Response<okhttp3.ResponseBody> response) {
                try {
                    swipeRefreshLayout.setRefreshing(false);
                    showLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        android.util.Log.d("ReportsListActivity", "Old raw reports body: " + body);
                        parseReportsResponse(body);
                    } else {
                        Toast.makeText(ReportsListActivity.this,
                                "Không thể tải dữ liệu báo cáo từ server.",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    android.util.Log.e("ReportsListActivity", "Parse old raw failed: " + e.getMessage());
                    Toast.makeText(ReportsListActivity.this,
                            "Không thể tải dữ liệu báo cáo từ server.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                showLoading(false);
                Toast.makeText(ReportsListActivity.this,
                        "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void parseReportsResponse(String responseString) {
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.google.gson.JsonObject root = gson.fromJson(responseString, com.google.gson.JsonObject.class);
            if (root != null) {
                if (root.has("data") && root.get("data").isJsonArray()) {
                    com.google.gson.JsonArray arr = root.getAsJsonArray("data");
                    List<Report> list = new ArrayList<>();
                    for (com.google.gson.JsonElement el : arr) {
                        if (!el.isJsonObject()) continue;
                        com.google.gson.JsonObject o = el.getAsJsonObject();
                        Report r = new Report();
                        if (o.has("reportId")) r.setReportId(o.get("reportId").getAsInt());
                        if (o.has("binId")) r.setBinId(o.get("binId").getAsInt());
                        if (o.has("accountId")) r.setAccountId(o.get("accountId").getAsInt());
                        if (o.has("reportType")) r.setReportType(o.get("reportType").getAsString());
                        if (o.has("description")) r.setDescription(o.get("description").getAsString());
                        if (o.has("status")) r.setStatus(o.get("status").getAsString());
                        if (o.has("createdAt") && o.get("createdAt").isJsonPrimitive()) {
                            try { r.setCreatedAt(new java.util.Date(o.get("createdAt").getAsLong())); } catch (Exception ignore) {}
                        }
                        if (o.has("resolvedAt") && o.get("resolvedAt").isJsonPrimitive()) {
                            try { r.setResolvedAt(new java.util.Date(o.get("resolvedAt").getAsLong())); } catch (Exception ignore) {}
                        }
                        if (o.has("binCode")) r.setBinCode(o.get("binCode").getAsString());
                        if (o.has("binAddress")) r.setBinAddress(o.get("binAddress").getAsString());
                        list.add(r);
                    }
                    allReports = list;
                    filterReports(tabLayout.getSelectedTabPosition());
                    return;
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ReportsListActivity", "parseReportsResponse failed: " + e.getMessage());
        }
        Toast.makeText(ReportsListActivity.this,
                "Không thể tải dữ liệu báo cáo từ server.",
                Toast.LENGTH_LONG).show();
    }

    private void filterReports(int tabPosition) {
        List<Report> filteredReports;
        switch (tabPosition) {
            case 1: // Đang xử lý
                filteredReports = new ArrayList<>();
                for (Report report : allReports) {
                    if ("RECEIVED".equals(report.getStatus()) ||
                        "ASSIGNED".equals(report.getStatus()) ||
                        "PROCESSING".equals(report.getStatus())) {
                        filteredReports.add(report);
                    }
                }
                break;
            case 2: // Hoàn thành
                filteredReports = new ArrayList<>();
                for (Report report : allReports) {
                    if ("DONE".equalsIgnoreCase(report.getStatus()) ||
                        "RESOLVED".equalsIgnoreCase(report.getStatus())) {
                        filteredReports.add(report);
                    }
                }
                break;
            default: // Tất cả
                filteredReports = allReports;
                break;
        }

        adapter.setReports(filteredReports);
        showEmpty(filteredReports.isEmpty());
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            rvReports.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
        } else {
            rvReports.setVisibility(View.VISIBLE);
        }
    }

    private void showEmpty(boolean show) {
        tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        rvReports.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onReportClick(Report report) {
        Intent intent = new Intent(this, ReportDetailActivity.class);
        intent.putExtra("report_id", report.getReportId());
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
