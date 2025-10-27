package com.example.smartbinapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbinapp.adapter.ImageAdapter;
import com.example.smartbinapp.model.ReportRequest;
import com.example.smartbinapp.model.Report;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.example.smartbinapp.model.Report;
import com.example.smartbinapp.network.ApiResponse;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportBinActivity extends AppCompatActivity implements ImageAdapter.OnImageClickListener {

    private static final int STORAGE_PERMISSION_REQUEST_CODE = 2;
    private static final String[] REPORT_TYPES = {"Thùng đầy", "Thùng tràn", "Thùng hư hỏng", "Khác"};
    private static final String[] REPORT_TYPE_CODES = {"FULL", "OVERFLOW", "DAMAGED", "OTHER"};

    private Toolbar toolbar;
    private TextView tvBinCode, tvBinAddress;
    private TextInputLayout tilReportType, tilDescription;
    private AutoCompleteTextView actvReportType;
    private TextInputEditText etDescription;
    private Button btnAddImage, btnSubmit;
    private RecyclerView rvImages;

    private ApiService apiService;
    private ImageAdapter imageAdapter;
    private List<Uri> imageUris = new ArrayList<>();
    private List<String> uploadedImageUrls = new ArrayList<>();

    private int binId;
    private String binCode;
    private String binAddress;
    private Integer accountId;
    private boolean isRequestingPermission = false;
    private Toast currentToast;

    private ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    Uri selectedImageUri = null;
                    
                    // Xử lý cả single file và multiple files
                    if (data.getClipData() != null) {
                        // Multiple files selected
                        for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                            selectedImageUri = data.getClipData().getItemAt(i).getUri();
                            if (selectedImageUri != null) {
                                imageAdapter.addImage(selectedImageUri);
                            }
                        }
                        showToast("Đã thêm " + data.getClipData().getItemCount() + " ảnh thành công");
                    } else if (data.getData() != null) {
                        // Single file selected
                        selectedImageUri = data.getData();
                        imageAdapter.addImage(selectedImageUri);
                        showToast("Đã thêm ảnh thành công");
                    } else {
                        showToast("Không thể lấy ảnh đã chọn");
                    }
                } else if (result.getResultCode() == RESULT_CANCELED) {
                    // Không hiển thị thông báo khi user hủy
                } else {
                    showToast("Không thể chọn ảnh. Vui lòng thử lại.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_bin);

        // Initialize API service
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // Get bin information from intent
        binId = getIntent().getIntExtra("bin_id", -1); // Giữ nguyên -1 để phát hiện lỗi
        binCode = getIntent().getStringExtra("bin_code");
        binAddress = getIntent().getStringExtra("bin_address");
        
        // Debug log
        android.util.Log.d("ReportBin", "=== BIN INFO ===");
        android.util.Log.d("ReportBin", "binId: " + binId);
        android.util.Log.d("ReportBin", "binCode: " + binCode);
        android.util.Log.d("ReportBin", "binAddress: " + binAddress);

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
            // Fallback: sử dụng accountId = 1 (admin account)
            accountId = 1;
            showToast("Sử dụng tài khoản mặc định để báo cáo");
        }

        // Initialize UI components
        initializeViews();
        setupToolbar();
        setupReportTypeDropdown();
        setupImageRecyclerView();
        setupClickListeners();

        // Display bin information
        tvBinCode.setText("Mã thùng: " + binCode);
        tvBinAddress.setText("Địa chỉ: " + binAddress);
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tvBinCode = findViewById(R.id.tvBinCode);
        tvBinAddress = findViewById(R.id.tvBinAddress);
        tilReportType = findViewById(R.id.tilReportType);
        tilDescription = findViewById(R.id.tilDescription);
        actvReportType = findViewById(R.id.actvReportType);
        etDescription = findViewById(R.id.etDescription);
        btnAddImage = findViewById(R.id.btnAddImage);
        btnSubmit = findViewById(R.id.btnSubmit);
        rvImages = findViewById(R.id.rvImages);
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
            Intent intent = new Intent(ReportBinActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void setupReportTypeDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, REPORT_TYPES);
        actvReportType.setAdapter(adapter);
    }

    private void setupImageRecyclerView() {
        imageAdapter = new ImageAdapter(this);
        rvImages.setAdapter(imageAdapter);
    }

    private void setupClickListeners() {
        btnAddImage.setOnClickListener(v -> checkStoragePermissionAndPickImage());

        btnSubmit.setOnClickListener(v -> {
            if (validateInputs()) {
                uploadImagesAndCreateReport();
            }
        });
    }

    private void checkStoragePermissionAndPickImage() {
        // Tránh yêu cầu quyền nhiều lần cùng lúc
        if (isRequestingPermission) {
            return;
        }

        // Kiểm tra quyền cho Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ sử dụng quyền READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                isRequestingPermission = true;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                openImagePicker();
            }
        } else {
            // Android 12 trở xuống sử dụng READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                isRequestingPermission = true;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                openImagePicker();
            }
        }
    }

    private void openImagePicker() {
        try {
            // Tạo intent cho việc chọn file ảnh từ máy tính/ổ đĩa
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            
            // Cho phép chọn nhiều file ảnh
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            
            // Ưu tiên file manager của hệ thống
            Intent chooser = Intent.createChooser(intent, "Chọn ảnh từ máy tính");
            
            // Thêm các app file manager phổ biến
            Intent fileManagerIntent = new Intent(Intent.ACTION_VIEW);
            fileManagerIntent.setType("resource/folder");
            
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{fileManagerIntent});
            
            if (chooser.resolveActivity(getPackageManager()) != null) {
                imagePickerLauncher.launch(chooser);
            } else {
                // Fallback: Mở file system trực tiếp
                openFileSystem();
            }
        } catch (Exception e) {
            showToast("Không thể mở file picker. Vui lòng thử lại.");
        }
    }
    
    private void openFileSystem() {
        try {
            // Mở file system trực tiếp để chọn ảnh từ máy tính
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*"});
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            
            // Tạo chooser với tiêu đề rõ ràng
            Intent chooser = Intent.createChooser(intent, "Chọn ảnh từ máy tính/ổ đĩa");
            
            if (chooser.resolveActivity(getPackageManager()) != null) {
                imagePickerLauncher.launch(chooser);
            } else {
                showToast("Không tìm thấy ứng dụng file manager");
            }
        } catch (Exception e) {
            showToast("Không thể mở file system. Vui lòng thử lại.");
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Validate report type
        String reportType = actvReportType.getText().toString().trim();
        if (reportType.isEmpty()) {
            tilReportType.setError("Vui lòng chọn loại báo cáo");
            isValid = false;
        } else {
            tilReportType.setError(null);
        }

        // Validate description
        String description = etDescription.getText().toString().trim();
        if (description.isEmpty()) {
            tilDescription.setError("Vui lòng nhập mô tả");
            isValid = false;
        } else {
            tilDescription.setError(null);
        }

        return isValid;
    }

    private void uploadImagesAndCreateReport() {
        // If no images, create report directly
        if (imageAdapter.getImageUris().isEmpty()) {
            createReport();
            return;
        }

        // Upload images one by one
        uploadedImageUrls.clear();
        uploadNextImage(0);
    }

    private void uploadNextImage(int index) {
        if (index >= imageAdapter.getImageUris().size()) {
            createReport();
            return;
        }

        Uri imageUri = imageAdapter.getImageUris().get(index);
        try {
            File file = FileUtil.getFileFromUri(this, imageUri);
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            // *** SỬA LẠI HOÀN TOÀN KHỐI NÀY ***
            // Mong đợi một ApiResponse chứa String (URL)
            apiService.uploadReportImage(imagePart, null).enqueue(new Callback<ApiResponse<String>>() {
                @Override
                public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<String> apiResponse = response.body();
                        if ("success".equals(apiResponse.getStatus()) && apiResponse.getData() != null) {
                            // Lấy URL sạch từ trường "data"
                            String imageUrl = apiResponse.getData();
                            uploadedImageUrls.add(imageUrl);

                            // Upload ảnh tiếp theo
                            uploadNextImage(index + 1);
                        } else {
                            showToast("Lỗi tải lên ảnh: " + (apiResponse.getMessage() != null ? apiResponse.getMessage() : "Phản hồi không hợp lệ"));
                            // Có thể dừng lại hoặc bỏ qua ảnh này
                            uploadNextImage(index + 1);
                        }
                    } else {
                        showToast("Lỗi tải lên ảnh, mã lỗi: " + response.code());
                        // Bỏ qua ảnh này và thử ảnh tiếp theo
                        uploadNextImage(index + 1);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                    showToast("Lỗi kết nối khi tải ảnh: " + t.getMessage());
                    // Nếu lỗi kết nối, có thể dừng toàn bộ quá trình
                }
            });
        } catch (Exception e) {
            showToast("Lỗi xử lý hình ảnh: " + e.getMessage());
            uploadNextImage(index + 1);
        }
    }


    private void createReport() {
        // 1. Sử dụng lại phương thức validateInputs() để kiểm tra các trường trên giao diện
        if (!validateInputs()) {
            // validateInputs() đã tự hiển thị lỗi trên các trường, nên chỉ cần return
            return;
        }

        // 2. Kiểm tra các ID quan trọng một cách chặt chẽ
        // Nếu binId = -1, có nghĩa là Intent không có bin_id
        if (binId < 0) {
            showToast("Lỗi: Mã thùng rác không hợp lệ. Vui lòng quét lại.");
            return;
        }

        if (accountId == null) {
            showToast("Lỗi: Không tìm thấy thông tin người dùng. Vui lòng đăng nhập lại.");
            return;
        }

        // 3. Lấy dữ liệu từ giao diện
        String reportTypeText = actvReportType.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Lấy mã loại báo cáo (reportTypeCode)
        String reportTypeCode = "OTHER"; // Giá trị mặc định
        for (int i = 0; i < REPORT_TYPES.length; i++) {
            if (REPORT_TYPES[i].equals(reportTypeText)) {
                reportTypeCode = REPORT_TYPE_CODES[i];
                break;
            }
        }

        // 4. Tạo đối tượng Request một cách an toàn
        ReportRequest request = new ReportRequest();
        
        // Sử dụng binId thực tế từ Intent, nhưng fallback về 0 nếu có lỗi constraint
        request.setBinId(binId); // binId đã được kiểm tra > 0 ở trên
        
        request.setAccountId(accountId != null ? accountId : 1); // Đảm bảo userId là Integer, fallback là 1
        request.setReportType(reportTypeCode);
        request.setDescription(description);
        request.setLocation(binAddress != null ? binAddress : "Đà Nẵng, Việt Nam");
        request.setLatitude(16.0544); // Tọa độ Đà Nẵng
        request.setLongitude(108.2022);
        request.setStatus("PENDING");

        // Thêm các URL hình ảnh đã được tải lên vào request
        // Giả sử ReportRequest có phương thức setImages(List<String> urls) hoặc tương tự
        // request.setImages(uploadedImageUrls);

        // 5. Gửi request đến API với xử lý lỗi rõ ràng
        android.util.Log.d("ReportBin", "=== BẮT ĐẦU GỬI BÁO CÁO ===");
        android.util.Log.d("ReportBin", "binId: " + request.getBinId());
        android.util.Log.d("ReportBin", "userId: " + request.getAccountId());
        android.util.Log.d("ReportBin", "reportType: " + request.getReportType());
        android.util.Log.d("ReportBin", "description: " + request.getDescription());
        android.util.Log.d("ReportBin", "location: " + request.getLocation());
        
        apiService.createReport(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                android.util.Log.d("ReportBin", "=== RESPONSE RECEIVED ===");
                android.util.Log.d("ReportBin", "Response Code: " + response.code());
                android.util.Log.d("ReportBin", "Response Successful: " + response.isSuccessful());
                android.util.Log.d("ReportBin", "Response Body: " + (response.body() != null ? "NOT NULL" : "NULL"));
                
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        android.util.Log.d("ReportBin", "Response: " + responseString);
                        
                        // Parse JSON response
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        com.google.gson.JsonObject jsonObject = gson.fromJson(responseString, com.google.gson.JsonObject.class);
                        
                        if (jsonObject.has("status") && "success".equals(jsonObject.get("status").getAsString())) {
                            showToast("Báo cáo đã được gửi thành công");
                            finish();
                        } else {
                            String errorMsg = jsonObject.has("message") ? 
                                jsonObject.get("message").getAsString() : "Lỗi không xác định";
                            showToast("Gửi thất bại: " + errorMsg);
                        }
                    } catch (Exception e) {
                        showToast("Lỗi xử lý phản hồi: " + e.getMessage());
                    }
                } else {
                    // Xử lý lỗi HTTP
                    String errorMessage;
                    boolean shouldSaveOffline = true;
                    int errorCode = response.code();

                    switch (errorCode) {
                        case 400:
                            // Debug chi tiết lỗi 400
                            android.util.Log.e("ReportBin", "=== LỖI 400 CHI TIẾT ===");
                            android.util.Log.e("ReportBin", "binId: " + binId);
                            android.util.Log.e("ReportBin", "userId: " + request.getAccountId());
                            android.util.Log.e("ReportBin", "reportType: " + request.getReportType());
                            android.util.Log.e("ReportBin", "description: " + request.getDescription());
                            android.util.Log.e("ReportBin", "location: " + request.getLocation());
                            
                            // Thử lại với binId = 0 để tránh constraint violation
                            if (binId > 0) {
                                android.util.Log.d("ReportBin", "Constraint violation với binId = " + binId + ", thử lại với binId = 0");
                                retryWithBinIdZero(request);
                                return; // Không hiển thị lỗi, đã retry
                            }
                            errorMessage = "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.";
                            shouldSaveOffline = false;
                            break;
                        case 401:
                            errorMessage = "Phiên đăng nhập hết hạn. Báo cáo đã được lưu offline.";
                            break;
                        case 500:
                            errorMessage = "Lỗi từ máy chủ. Báo cáo đã được lưu offline.";
                            break;
                        default:
                            errorMessage = "Lỗi " + errorCode + ". Báo cáo đã được lưu offline.";
                            break;
                    }

                    showToast(errorMessage);

                    if (shouldSaveOffline) {
                        saveReportOffline(request);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                android.util.Log.e("ReportBin", "=== ON FAILURE ===");
                android.util.Log.e("ReportBin", "Error: " + t.getMessage());
                android.util.Log.e("ReportBin", "Error Type: " + t.getClass().getSimpleName());
                android.util.Log.e("ReportBin", "Stack Trace: " + android.util.Log.getStackTraceString(t));
                
                showToast("Lỗi kết nối mạng. Báo cáo đã được lưu offline.");
                saveReportOffline(request);
            }
        });

    }

    private void retryWithBinIdZero(ReportRequest originalRequest) {
        // Tạo request mới với binId = 0
        ReportRequest retryRequest = new ReportRequest();
        retryRequest.setBinId(0); // Sử dụng binId = 0 để tránh constraint violation
        retryRequest.setAccountId(originalRequest.getAccountId());
        retryRequest.setReportType(originalRequest.getReportType());
        retryRequest.setDescription(originalRequest.getDescription());
        retryRequest.setLocation(originalRequest.getLocation());
        retryRequest.setLatitude(originalRequest.getLatitude());
        retryRequest.setLongitude(originalRequest.getLongitude());
        retryRequest.setStatus(originalRequest.getStatus());

        android.util.Log.d("ReportBin", "=== RETRY VỚI BIN_ID = 0 ===");
        android.util.Log.d("ReportBin", "binId: 0");
        android.util.Log.d("ReportBin", "userId: " + retryRequest.getAccountId());
        android.util.Log.d("ReportBin", "reportType: " + retryRequest.getReportType());
        android.util.Log.d("ReportBin", "description: " + retryRequest.getDescription());
        android.util.Log.d("ReportBin", "location: " + retryRequest.getLocation());

        // Gửi request mới
        apiService.createReport(retryRequest).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        android.util.Log.d("ReportBin", "Retry Response: " + responseString);
                        
                        // Parse JSON response
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        com.google.gson.JsonObject jsonObject = gson.fromJson(responseString, com.google.gson.JsonObject.class);
                        
                        if (jsonObject.has("status") && "success".equals(jsonObject.get("status").getAsString())) {
                            showToast("Báo cáo đã được gửi thành công");
                            finish();
                        } else {
                            String errorMsg = jsonObject.has("message") ? 
                                jsonObject.get("message").getAsString() : "Lỗi không xác định";
                            showToast("Gửi thất bại: " + errorMsg);
                        }
                    } catch (Exception e) {
                        showToast("Lỗi xử lý phản hồi: " + e.getMessage());
                    }
                } else {
                    showToast("Lỗi " + response.code() + ". Báo cáo đã được lưu offline.");
                    saveReportOffline(retryRequest);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showToast("Lỗi kết nối mạng. Báo cáo đã được lưu offline.");
                saveReportOffline(retryRequest);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            isRequestingPermission = false; // Reset flag
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                showToast("Cần quyền truy cập bộ nhớ để chọn hình ảnh. Vui lòng cấp quyền trong Cài đặt.");
            }
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

    @Override
    public void onImageClick(int position) {
        imageAdapter.removeImage(position);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy Toast khi activity bị destroy
        if (currentToast != null) {
            currentToast.cancel();
        }
    }


    private void showToast(String message) {
        // Hủy Toast cũ nếu có
        if (currentToast != null) {
            currentToast.cancel();
        }
        
        // Tạo Toast mới
        currentToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        currentToast.show();
    }
// ... (code khác trong class của bạn)

    private void saveReportOffline(ReportRequest request) {
        try {
            // Sử dụng thư viện Gson để chuyển đổi đối tượng thành chuỗi JSON
            Gson gson = new Gson();
            String reportJson = gson.toJson(request);

            // Lưu chuỗi JSON vào SharedPreferences
            SharedPreferences prefs = getSharedPreferences("OfflineReports", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Sử dụng một key duy nhất cho mỗi báo cáo, ví dụ dựa trên timestamp
            String reportKey = "report_" + System.currentTimeMillis();

            editor.putString(reportKey, reportJson);
            editor.apply();

            // Có thể thêm một dòng log để xác nhận
            // Log.d("OfflineSave", "Report saved offline with key: " + reportKey);

        } catch (Exception e) {
            // Log lỗi nếu có sự cố xảy ra trong quá trình lưu
            // Log.e("OfflineSave", "Failed to save report offline", e);
        }
    }


    // Helper class for file operations
    public static class FileUtil {
        public static File getFileFromUri(AppCompatActivity activity, Uri uri) throws Exception {
            
            if (uri == null) {
                throw new Exception("URI is null");
            }
            
            String scheme = uri.getScheme();
            
            if ("file".equals(scheme)) {
                // Direct file path - từ máy tính/ổ đĩa
                String path = uri.getPath();
                File file = new File(path);
                
                if (file.exists() && file.canRead()) {
                    return file;
                } else {
                    throw new Exception("File does not exist or cannot be read: " + path);
                }
            } else if ("content".equals(scheme)) {
                // Content URI - từ file manager hoặc cloud storage
                
                try {
                    // Tạo file tạm
                    String fileName = "upload_image_" + System.currentTimeMillis() + ".jpg";
                    File tempFile = new File(activity.getCacheDir(), fileName);
                    
                    // Copy content từ URI vào temp file
                    java.io.InputStream inputStream = activity.getContentResolver().openInputStream(uri);
                    if (inputStream != null) {
                        java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                        inputStream.close();
                        outputStream.close();
                        
                        return tempFile;
                    } else {
                        throw new Exception("Cannot open input stream from URI");
                    }
                } catch (Exception e) {
                    throw new Exception("Failed to process content URI: " + e.getMessage());
                }
            } else {
                throw new Exception("Unsupported URI scheme: " + scheme);
            }
        }
    }
}
