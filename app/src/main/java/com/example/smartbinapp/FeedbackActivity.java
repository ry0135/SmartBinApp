package com.example.smartbinapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbinapp.adapter.ResolvedReportsAdapter;
import com.example.smartbinapp.model.Feedback;
import com.example.smartbinapp.model.Report;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedbackActivity extends AppCompatActivity implements ResolvedReportsAdapter.OnReportClickListener {

    private Toolbar toolbar;
    private RecyclerView rvResolvedReports;
    private RatingBar ratingBar;
    private TextView tvRatingDescription, tvSelectedReport;
    private TextInputLayout tilComment;
    private TextInputEditText etComment;
    private Button btnSubmit, btnRetry;
    private ProgressBar progressBar;

    private ApiService apiService;
    private Integer accountId;
    private Integer selectedReportId = null;
    private String selectedReportDescription = "";
    private ResolvedReportsAdapter reportsAdapter;
    private List<Report> resolvedReports = new ArrayList<>();
    private int retryCount = 0;
    private static final int MAX_RETRY = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        // Initialize API service
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // Get account ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userIdStr = prefs.getString("userId", null);
        if (userIdStr != null) {
            try {
                accountId = Integer.parseInt(userIdStr);
            } catch (NumberFormatException e) {
                showToast("Lỗi xác thực người dùng");
                finish();
                return;
            }
        } else {  
            showToast("Vui lòng đăng nhập để đánh giá");
            finish();
            return;
        }

        // Initialize UI components
        initializeViews();
        setupToolbar();
        setupClickListeners();
        setupRecyclerView();

        // Load resolved reports
        loadResolvedReports();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        rvResolvedReports = findViewById(R.id.rvResolvedReports);
        ratingBar = findViewById(R.id.ratingBar);
        tvRatingDescription = findViewById(R.id.tvRatingDescription);
        tvSelectedReport = findViewById(R.id.tvSelectedReport);
        tilComment = findViewById(R.id.tilComment);
        etComment = findViewById(R.id.etComment);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnRetry = findViewById(R.id.btnRetry);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Đánh giá báo cáo");
        }
    }

    private void setupRecyclerView() {
        reportsAdapter = new ResolvedReportsAdapter(resolvedReports, this);
        rvResolvedReports.setLayoutManager(new LinearLayoutManager(this));
        rvResolvedReports.setAdapter(reportsAdapter);
    }

    private void setupClickListeners() {
        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> updateRatingDescription((int) rating));

        btnSubmit.setOnClickListener(v -> {
            if (validateInputs()) {
                submitFeedback();
            }
        });

        btnRetry.setOnClickListener(v -> {
            btnRetry.setVisibility(View.GONE);
            submitFeedback();
        });
    }

    private void loadResolvedReports() {
        showLoading(true);
        android.util.Log.d("FeedbackActivity", "Loading reports for accountId: " + accountId);

        // Thử gọi raw trước để tự parse {status, message, data}
        apiService.getUserReportsRaw(String.valueOf(accountId)).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                try {
                    showLoading(false);
                    android.util.Log.d("FeedbackActivity", "Raw reports Response Code: " + response.code());
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        android.util.Log.d("FeedbackActivity", "Raw reports body: " + body);
                        parseReportsResponse(body);
                    } else {
                        android.util.Log.w("FeedbackActivity", "Raw reports failed, trying typed endpoint");
                        loadResolvedReportsTyped();
                    }
                } catch (Exception e) {
                    android.util.Log.e("FeedbackActivity", "Raw parse failed: " + e.getMessage());
                    loadResolvedReportsTyped();
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                showLoading(false);
                android.util.Log.e("FeedbackActivity", "Raw reports call failed: " + t.getMessage());
                loadResolvedReportsTyped();
            }
        });
    }

    private void loadResolvedReportsTyped() {
        showLoading(true);
        apiService.getUserReports(String.valueOf(accountId)).enqueue(new Callback<List<Report>>() {
            @Override
            public void onResponse(Call<List<Report>> call, Response<List<Report>> response) {
                showLoading(false);
                android.util.Log.d("FeedbackActivity", "Typed reports Response Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    applyReportsList(response.body());
                } else {
                    android.util.Log.w("FeedbackActivity", "Typed endpoint failed, trying old raw endpoint");
                    tryOldEndpointRaw();
                }
            }

            @Override
            public void onFailure(Call<List<Report>> call, Throwable t) {
                showLoading(false);
                android.util.Log.e("FeedbackActivity", "Typed call failed: " + t.getMessage());
                tryOldEndpointRaw();
            }
        });
    }

    private void tryOldEndpointRaw() {
        showLoading(true);
        apiService.getUserReportsOldRaw(String.valueOf(accountId)).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                try {
                    showLoading(false);
                    android.util.Log.d("FeedbackActivity", "Old raw reports Response Code: " + response.code());
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        android.util.Log.d("FeedbackActivity", "Old raw body: " + body);
                        parseReportsResponse(body);
                    } else {
                        android.util.Log.w("FeedbackActivity", "Old raw failed, trying old typed endpoint");
                        tryOldEndpoint();
                    }
                } catch (Exception e) {
                    android.util.Log.e("FeedbackActivity", "Old raw parse failed: " + e.getMessage());
                    tryOldEndpoint();
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                showLoading(false);
                android.util.Log.e("FeedbackActivity", "Old raw call failed: " + t.getMessage());
                tryOldEndpoint();
            }
        });
    }

    private void parseReportsResponse(String responseString) {
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.google.gson.JsonObject root = gson.fromJson(responseString, com.google.gson.JsonObject.class);
            if (root != null && root.has("data") && root.get("data").isJsonArray()) {
                com.google.gson.JsonArray arr = root.getAsJsonArray("data");
                java.util.List<Report> list = new java.util.ArrayList<>();
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
                    // Optional fields
                    if (o.has("binCode")) r.setBinCode(o.get("binCode").getAsString());
                    if (o.has("binAddress")) r.setBinAddress(o.get("binAddress").getAsString());
                    list.add(r);
                }
                applyReportsList(list);
                return;
            }
        } catch (Exception e) {
            android.util.Log.e("FeedbackActivity", "parseReportsResponse failed: " + e.getMessage());
        }
        showToast("Không thể tải danh sách báo cáo");
    }

    private void applyReportsList(java.util.List<Report> reports) {
        // Lọc chỉ lấy reports có status = RESOLVED hoặc DONE (tùy server)
        resolvedReports.clear();
        for (Report report : reports) {
            android.util.Log.d("FeedbackActivity", "Report ID: " + report.getReportId() + ", Status: " + report.getStatus());
            if ("RESOLVED".equalsIgnoreCase(report.getStatus()) || "DONE".equalsIgnoreCase(report.getStatus())) {
                resolvedReports.add(report);
            }
        }
        android.util.Log.d("FeedbackActivity", "Resolved reports size: " + resolvedReports.size());
        reportsAdapter.notifyDataSetChanged();
        if (resolvedReports.isEmpty()) {
            showToast("Bạn chưa có báo cáo nào được xử lý để đánh giá");
        }
    }

    private void tryOldEndpoint() {
        android.util.Log.d("FeedbackActivity", "Trying old endpoint for reports");
        showLoading(true);
        
        apiService.getUserReportsOld(String.valueOf(accountId)).enqueue(new Callback<List<Report>>() {
            @Override
            public void onResponse(Call<List<Report>> call, Response<List<Report>> response) {
                showLoading(false);
                android.util.Log.d("FeedbackActivity", "Old endpoint response code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    android.util.Log.d("FeedbackActivity", "Old endpoint received " + response.body().size() + " reports");
                    
                    resolvedReports.clear();
                    for (Report report : response.body()) {
                        if ("RESOLVED".equalsIgnoreCase(report.getStatus())) {
                            resolvedReports.add(report);
                        }
                    }
                    
                    reportsAdapter.notifyDataSetChanged();
                    
                    if (resolvedReports.isEmpty()) {
                        showToast("Bạn chưa có báo cáo nào được xử lý để đánh giá");
                    } else {
                        showToast("Đã tải danh sách báo cáo (endpoint cũ)");
                    }
                } else {
                    showToast("Không thể tải danh sách báo cáo từ cả hai endpoint");
                }
            }

            @Override
            public void onFailure(Call<List<Report>> call, Throwable t) {
                showLoading(false);
                android.util.Log.e("FeedbackActivity", "Old endpoint also failed: " + t.getMessage());
                showToast("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    @Override
    public void onReportClick(Report report) {

        selectedReportId = report.getReportId();
        selectedReportDescription = report.getDescription();
        
        // Hiển thị thông tin report đã chọn
        tvSelectedReport.setText("Đã chọn: " + report.getDescription());
        tvSelectedReport.setVisibility(View.VISIBLE);
        
        // Hiển thị form đánh giá
        findViewById(R.id.feedbackForm).setVisibility(View.VISIBLE);
        
        showToast("Đã chọn báo cáo để đánh giá");
    }

    private void updateRatingDescription(int rating) {
        String description;
        switch (rating) {
            case 1:
                description = "Rất không hài lòng";
                break;
            case 2:
                description = "Không hài lòng";
                break;
            case 3:
                description = "Bình thường";
                break;
            case 4:
                description = "Hài lòng";
                break;
            case 5:
                description = "Rất hài lòng";
                break;
            default:
                description = "Hãy chọn số sao đánh giá";
                break;
        }
        tvRatingDescription.setText(description);
    }

    private boolean validateInputs() {
        if (selectedReportId == null) {
            showToast("Vui lòng chọn báo cáo để đánh giá");
            return false;
        }
        
        int rating = (int) ratingBar.getRating();
        if (rating == 0) {
            showToast("Vui lòng chọn số sao đánh giá");
            return false;
        }
        
        String comment = etComment.getText().toString().trim();
        if (comment.isEmpty()) {
            tilComment.setError("Vui lòng nhập nhận xét");
            return false;
        } else {
            tilComment.setError(null);
        }
        
        return true;
    }

    private void submitFeedback() {
        submitFeedbackWithRetry();
    }

    private void submitFeedbackWithRetry() {
        showLoading(true);

        Feedback feedback = new Feedback();
        feedback.setAccountId(accountId);
        feedback.setWardId(1); // Default ward ID - có thể lấy từ report
        feedback.setRating((int) ratingBar.getRating());
        feedback.setComment(etComment.getText().toString().trim());
        feedback.setReportId(selectedReportId); // Liên kết với report đã chọn

        // Validate feedback before sending
        if (!feedback.isValid()) {
            String error = feedback.getValidationError();
            showToast("Lỗi dữ liệu: " + error);
            showLoading(false);
            return;
        }

        // Log feedback data for debugging

        apiService.createFeedback(feedback).enqueue(new Callback<Feedback>() {
            @Override
            public void onResponse(Call<Feedback> call, Response<Feedback> response) {
                showLoading(false);
                android.util.Log.d("FeedbackActivity", "Feedback response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    showToast("Đánh giá đã được gửi thành công");
                    finish();
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                            android.util.Log.e("FeedbackActivity", "Error response body: " + errorBody);
                        }
                    } catch (Exception e) {
                        errorBody = "Không thể đọc lỗi chi tiết";
                    }

                    String errorMessage = "Lỗi khi gửi đánh giá";
                    if (response.code() == 400) {
                        errorMessage = "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại thông tin.";
                        // Thử gửi với format khác nếu lỗi 400
                        tryAlternativeFeedbackFormat(feedback);
                    } else if (response.code() == 401) {
                        errorMessage = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
                        saveFeedbackLocally(feedback);
                    } else if (response.code() == 404) {
                        errorMessage = "Endpoint không tồn tại. Đã lưu đánh giá offline.";
                        android.util.Log.w("FeedbackActivity", "Feedback endpoint 404, saving locally");
                        saveFeedbackLocally(feedback);
                    } else if (response.code() == 500) {
                        errorMessage = "Lỗi server. Vui lòng thử lại sau.";
                        saveFeedbackLocally(feedback);
                    } else {
                        saveFeedbackLocally(feedback);
                    }

                    if (response.code() != 400) {
                        showToast(errorMessage + " (Mã lỗi: " + response.code() + ")");
                    }
                }
            }

            @Override
            public void onFailure(Call<Feedback> call, Throwable t) {
                showLoading(false);

                // Thử lại nếu chưa đạt max retry
                if (retryCount < MAX_RETRY) {
                    retryCount++;
                    // Delay 2 giây trước khi retry
                    new android.os.Handler().postDelayed(() -> submitFeedbackWithRetry(), 2000);
                } else {
                    // Lưu feedback offline và hiển thị nút retry
                    saveFeedbackLocally(feedback);
                    btnRetry.setVisibility(View.VISIBLE);
                    showToast("Lỗi kết nối. Đã lưu đánh giá offline để gửi lại sau.");
                }
            }
        });
    }

    private void tryAlternativeFeedbackFormat(Feedback originalFeedback) {
        try {
            // Tạo feedback với format khác
            Feedback alternativeFeedback = new Feedback();
            alternativeFeedback.setAccountId(originalFeedback.getAccountId());
            alternativeFeedback.setWardId(originalFeedback.getWardId());
            alternativeFeedback.setRating(originalFeedback.getRating());
            alternativeFeedback.setComment(originalFeedback.getComment());
            alternativeFeedback.setReportId(originalFeedback.getReportId());


            apiService.createFeedback(alternativeFeedback).enqueue(new Callback<Feedback>() {
                @Override
                public void onResponse(Call<Feedback> call, Response<Feedback> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        showToast("Đánh giá đã được gửi thành công (format thay thế)");
                        finish();
                    } else {
                        saveFeedbackLocally(originalFeedback);
                        showToast("Không thể gửi đánh giá. Đã lưu offline để gửi lại sau.");
                    }
                }

                @Override
                public void onFailure(Call<Feedback> call, Throwable t) {
                    saveFeedbackLocally(originalFeedback);
                    showToast("Lỗi kết nối. Đã lưu đánh giá offline.");
                }
            });
        } catch (Exception e) {
            saveFeedbackLocally(originalFeedback);
            showToast("Lỗi xử lý. Đã lưu đánh giá offline.");
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!show);
    }

    private void saveFeedbackLocally(Feedback feedback) {
        try {
            // Lưu feedback vào SharedPreferences để có thể gửi lại sau
            SharedPreferences prefs = getSharedPreferences("OfflineFeedback", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            String feedbackKey = "feedback_" + System.currentTimeMillis();
            editor.putInt(feedbackKey + "_accountId", feedback.getAccountId());
            editor.putInt(feedbackKey + "_wardId", feedback.getWardId());
            editor.putInt(feedbackKey + "_rating", feedback.getRating());
            editor.putString(feedbackKey + "_comment", feedback.getComment());
            editor.putInt(feedbackKey + "_reportId", feedback.getReportId());
            editor.putLong(feedbackKey + "_timestamp", System.currentTimeMillis());

            editor.apply();
            
            android.util.Log.d("FeedbackActivity", "Feedback saved locally with key: " + feedbackKey);
            showToast("Đánh giá đã được lưu offline. Sẽ gửi lại khi có kết nối mạng.");

        } catch (Exception e) {
            android.util.Log.e("FeedbackActivity", "Failed to save feedback locally: " + e.getMessage());
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}