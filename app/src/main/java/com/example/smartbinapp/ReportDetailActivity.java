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
import com.example.smartbinapp.network.ApiResponse;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
 

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
    private View stepContainer;
    private TextView step1;
    private TextView step2;
    private TextView step3;
    private TextView step4;
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
        stepContainer = findViewById(R.id.stepContainer);
        step1 = findViewById(R.id.step1);
        step2 = findViewById(R.id.step2);
        step3 = findViewById(R.id.step3);
        step4 = findViewById(R.id.step4);
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
        // Không cần cấu hình đặc biệt cho chỉ báo bước đơn giản
    }

    private void setupImagesRecyclerView() {
        imagesAdapter = new ReportImagesAdapter(this);
        rvImages.setAdapter(imagesAdapter);
    }

    private void loadReportDetails() {
        showLoading(true);
        apiService.getReportDetails(reportId).enqueue(new Callback<ApiResponse<Report>>() {
            @Override
            public void onResponse(Call<ApiResponse<Report>> call, Response<ApiResponse<Report>> response) {

                showLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {

                    report = response.body().getData();   // <-- LẤY ĐÚNG DỮ LIỆU REPORT
                    displayReportDetails();

                } else {
                    Toast.makeText(ReportDetailActivity.this, "Không tải được báo cáo!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Report>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(ReportDetailActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
        if (report.getImages() != null && !report.getImages().isEmpty()) {
            imagesAdapter.setImageUrls(report.getImages());
            tvNoImages.setVisibility(View.GONE);
        } else {
            tvNoImages.setVisibility(View.VISIBLE);
        }

        // Update step indicators based on status
        updateStepView();

        // Show rate button if report is done
        // Kiểm tra trạng thái báo cáo là DONE
        if (report.getStatus() != null && "DONE".equals(report.getStatus())) {
            btnRate.setVisibility(View.VISIBLE);

            // Kiểm tra xem đã đánh giá hay chưa (biến isReviewed từ API trả về)
            if (report.isReviewed()) {
                // TRƯỜNG HỢP 1: Đã hoàn thành và ĐÃ ĐÁNH GIÁ
                btnRate.setText("Đã đánh giá");       // Đổi tên nút
                btnRate.setEnabled(false);            // Vô hiệu hóa nút (không cho bấm)

                // (Tùy chọn) Đổi màu nút sang xám để người dùng biết là không bấm được
                // btnRate.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                // Hoặc set background resource khác: btnRate.setBackgroundResource(R.drawable.btn_disabled);
                btnRate.setAlpha(0.5f); // Cách nhanh nhất để làm mờ nút đi
            } else {
                // TRƯỜNG HỢP 2: Đã hoàn thành nhưng CHƯA ĐÁNH GIÁ
                btnRate.setText("Đánh giá dịch vụ");  // Đảm bảo tên nút đúng
                btnRate.setEnabled(true);             // Cho phép bấm
                btnRate.setAlpha(1.0f);               // Đậm lên lại

                btnRate.setOnClickListener(v -> {
                    Intent intent = new Intent(ReportDetailActivity.this, FeedbackActivity.class);
                    intent.putExtra("report_id", reportId);
                    // Kiểm tra null an toàn hơn cho binId
                    intent.putExtra("bin_id", report.getBinId() != null ? report.getBinId() : -1);
                    // Gửi kèm mô tả để màn Feedback hiển thị tên báo cáo
                    intent.putExtra("report_description",
                            report.getDescription() != null ? report.getDescription() : "");
                    startActivity(intent);
                });
            }

        } else {
            // TRƯỜNG HỢP 3: Chưa hoàn thành (PENDING, PROCESSING...)
            btnRate.setVisibility(View.GONE);
        }
    }

    private void updateStepView() {
        try {
            if (stepContainer == null || step1 == null || step2 == null || step3 == null || step4 == null) {
                return;
            }

            if (report == null || report.getStatus() == null) {
                stepContainer.setVisibility(View.GONE);
                return;
            }

            int currentStepIndex;
            String status = report.getStatus();

            switch (status) {
                case "RECEIVED":
                    currentStepIndex = 1;
                    break;
                case "ASSIGNED":
                    currentStepIndex = 2;
                    break;
                case "PROCESSING":
                    currentStepIndex = 3;
                    break;
                case "DONE":
                    currentStepIndex = 4;
                    break;
                case "CANCELLED":
                    stepContainer.setVisibility(View.GONE);
                    return;
                default:
                    currentStepIndex = 0;
                    break;
            }

            TextView[] steps = new TextView[]{step1, step2, step3, step4};
            for (int i = 0; i < steps.length; i++) {
                int colorRes;
                if (i < currentStepIndex) {
                    colorRes = android.R.color.holo_green_dark; // đã hoàn thành
                } else if (i == currentStepIndex) {
                    colorRes = android.R.color.holo_blue_dark; // đang ở bước này
                } else {
                    colorRes = android.R.color.darker_gray; // chưa tới
                }
                try {
                    steps[i].getBackground().setTint(ContextCompat.getColor(this, colorRes));
                } catch (Exception e) {
                    steps[i].setBackgroundColor(ContextCompat.getColor(this, colorRes));
                }
            }
        } catch (Exception e) {
            System.out.println("Error updating step indicators: " + e.getMessage());
            if (stepContainer != null) {
                stepContainer.setVisibility(View.GONE);
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
