package com.example.smartbinapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbinapp.adapter.ReportImagesAdapter;
import com.example.smartbinapp.model.Report;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.example.smartbinapp.utils.StepView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportDetailActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvStatus;
    private TextView tvReportType;
    private TextView tvCreatedDate;
    private TextView tvDescription;
    private TextView tvBinCode;
    private TextView tvBinAddress;
    private RecyclerView rvImages;
    private TextView tvNoImages;
    private CardView cardImages;
    private StepView stepView;
    private Button btnRate;
    private ProgressBar progressBar;

    private ApiService apiService;
    private ReportImagesAdapter imagesAdapter;
    private int reportId;
    private Report report;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        // Initialize API service
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // Get report ID from intent
        reportId = getIntent().getIntExtra("report_id", -1);
        if (reportId == -1) {
            Toast.makeText(this, "Không tìm thấy thông tin báo cáo", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components
        initializeViews();
        setupToolbar();
        setupStepView();
        setupImagesRecyclerView();

        // Load report details
        loadReportDetails();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tvStatus = findViewById(R.id.tvStatus);
        tvReportType = findViewById(R.id.tvReportType);
        tvCreatedDate = findViewById(R.id.tvCreatedDate);
        tvDescription = findViewById(R.id.tvDescription);
        tvBinCode = findViewById(R.id.tvBinCode);
        tvBinAddress = findViewById(R.id.tvBinAddress);
        rvImages = findViewById(R.id.rvImages);
        tvNoImages = findViewById(R.id.tvNoImages);
        cardImages = findViewById(R.id.cardImages);
        stepView = findViewById(R.id.stepView);
        btnRate = findViewById(R.id.btnRate);
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
            Intent intent = new Intent(ReportDetailActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void setupStepView() {
        try {
            if (stepView != null) {
                List<String> steps = Arrays.asList("Tiếp nhận", "Phân công", "Xử lý", "Hoàn thành");
                stepView.setSteps(steps);
            }
        } catch (Exception e) {
            System.out.println("Error setting up step view: " + e.getMessage());
        }
    }

    private void setupImagesRecyclerView() {
        imagesAdapter = new ReportImagesAdapter(this);
        rvImages.setAdapter(imagesAdapter);
    }

    private void loadReportDetails() {
        showLoading(true);
        apiService.getReportDetails(reportId).enqueue(new Callback<Report>() {
            @Override
            public void onResponse(Call<Report> call, Response<Report> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    report = response.body();
                    displayReportDetails();
                } else {
                    Toast.makeText(ReportDetailActivity.this,
                            "Lỗi tải chi tiết báo cáo: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Report> call, Throwable t) {
                showLoading(false);
                Toast.makeText(ReportDetailActivity.this,
                        "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayReportDetails() {
        if (report == null) {
            Toast.makeText(this, "Không có dữ liệu báo cáo", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Set status with appropriate color
        tvStatus.setText(report.getStatusVietnamese());
        int statusColor;
        String statusValue = report.getStatus();
        if (statusValue == null) {
            statusColor = android.R.color.darker_gray;
        } else {
            switch (statusValue) {
                case "RECEIVED":
                    statusColor = android.R.color.holo_blue_dark;
                    break;
                case "ASSIGNED":
                    statusColor = android.R.color.holo_orange_dark;
                    break;
                case "PROCESSING":
                    statusColor = android.R.color.holo_orange_light;
                    break;
                case "DONE":
                    statusColor = android.R.color.holo_green_dark;
                    break;
                case "CANCELLED":
                    statusColor = android.R.color.darker_gray;
                    break;
                default:
                    statusColor = android.R.color.holo_blue_dark;
                    break;
            }
        }
        try {
            tvStatus.getBackground().setTint(ContextCompat.getColor(this, statusColor));
        } catch (Exception e) {
            // Xử lý trường hợp background null hoặc không hỗ trợ tint
            tvStatus.setBackgroundColor(ContextCompat.getColor(this, statusColor));
        }

        // Set report type
        String reportType;
        String reportTypeValue = report.getReportType();
        if (reportTypeValue == null) {
            reportType = "Khác";
        } else {
            switch (reportTypeValue) {
                case "FULL":
                    reportType = "Thùng đầy";
                    break;
                case "OVERFLOW":
                    reportType = "Thùng tràn";
                    break;
                case "DAMAGED":
                    reportType = "Thùng hư hỏng";
                    break;
                default:
                    reportType = "Khác";
                    break;
            }
        }
        tvReportType.setText("Loại báo cáo: " + reportType);

        // Set created date
        if (report.getCreatedAt() != null) {
            tvCreatedDate.setText("Ngày tạo: " + dateFormat.format(report.getCreatedAt()));
        }

        // Set description
        tvDescription.setText(report.getDescription() != null ? report.getDescription() : "");

        // Set bin info
        tvBinCode.setText("Mã thùng: " + (report.getBinCode() != null ? report.getBinCode() : ""));
        
        String address = "";
        if (report.getBinAddress() != null && !report.getBinAddress().isEmpty()) {
            address = report.getBinAddress();
        }
        tvBinAddress.setText("Địa chỉ: " + address);

        // Set images
        if (report.getImageUrls() != null && !report.getImageUrls().isEmpty()) {
            imagesAdapter.setImageUrls(report.getImageUrls());
            tvNoImages.setVisibility(View.GONE);
        } else {
            tvNoImages.setVisibility(View.VISIBLE);
        }

        // Update step view based on status
        updateStepView();

        // Show rate button if report is done
        if (report.getStatus() != null && "DONE".equals(report.getStatus())) {
            btnRate.setVisibility(View.VISIBLE);
            btnRate.setOnClickListener(v -> {
                Intent intent = new Intent(ReportDetailActivity.this, FeedbackActivity.class);
                intent.putExtra("report_id", reportId);
                intent.putExtra("bin_id", report.getBinId() != null ? report.getBinId() : -1);
                startActivity(intent);
            });
        } else {
            btnRate.setVisibility(View.GONE);
        }
    }

    private void updateStepView() {
        try {
            if (stepView == null) {
                System.out.println("StepView is null");
                return;
            }
            
            if (report == null || report.getStatus() == null) {
                stepView.setVisibility(View.GONE);
                return;
            }
            
            int currentStep = 0;
            String status = report.getStatus();
            
            switch (status) {
                case "RECEIVED":
                    currentStep = 0;
                    break;
                case "ASSIGNED":
                    currentStep = 1;
                    break;
                case "PROCESSING":
                    currentStep = 2;
                    break;
                case "DONE":
                    currentStep = 3;
                    break;
                case "CANCELLED":
                    // For cancelled, we don't show progress
                    if (stepView != null) {
                        stepView.setVisibility(View.GONE);
                    }
                    return;
                default:
                    // Trạng thái không xác định, giữ ở bước đầu tiên
                    currentStep = 0;
                    break;
            }
            
            try {
                stepView.go(currentStep, true);
                stepView.done(true);
            } catch (Exception e) {
                System.out.println("Error calling StepView methods: " + e.getMessage());
            }
        } catch (Exception e) {
            // Xử lý ngoại lệ nếu có
            System.out.println("Error updating step view: " + e.getMessage());
            if (stepView != null) {
                try {
                    stepView.setVisibility(View.GONE);
                } catch (Exception ex) {
                    System.out.println("Error hiding step view: " + ex.getMessage());
                }
            }
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
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
