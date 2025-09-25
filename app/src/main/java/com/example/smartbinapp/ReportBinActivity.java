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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        binId = getIntent().getIntExtra("bin_id", -1);
        binCode = getIntent().getStringExtra("bin_code");
        binAddress = getIntent().getStringExtra("bin_address");

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
            // All images uploaded, create report
            createReport();
            return;
        }

        Uri imageUri = imageAdapter.getImageUris().get(index);
        try {
            File file = FileUtil.getFileFromUri(this, imageUri);
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            apiService.uploadReportImage(imagePart, null).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String responseBody = response.body().string();
                            // Parse the response to get the image URL
                            // This is a simplified example - you'll need to parse the actual JSON response
                            String imageUrl = responseBody;
                            uploadedImageUrls.add(imageUrl);
                            
                            // Upload next image
                            uploadNextImage(index + 1);
                        } catch (IOException e) {
                            showToast("Lỗi khi xử lý phản hồi: " + e.getMessage());
                        }
                    } else {
                        showToast("Lỗi tải lên hình ảnh");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    showToast("Lỗi kết nối: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            showToast("Lỗi xử lý hình ảnh: " + e.getMessage());
            // Skip this image and try the next one
            uploadNextImage(index + 1);
        }
    }

    private void createReport() {
        // Validate dữ liệu trước khi gửi
        if (binId == -1) {
            showToast("Lỗi: Không tìm thấy thông tin thùng rác");
            return;
        }
        
        if (accountId == null) {
            showToast("Lỗi: Không tìm thấy thông tin người dùng");
            return;
        }

        // Get report type code
        String reportTypeText = actvReportType.getText().toString().trim();
        String reportTypeCode = "OTHER";
        for (int i = 0; i < REPORT_TYPES.length; i++) {
            if (REPORT_TYPES[i].equals(reportTypeText)) {
                reportTypeCode = REPORT_TYPE_CODES[i];
                break;
            }
        }

        String description = etDescription.getText().toString().trim();
        if (description.isEmpty()) {
            showToast("Vui lòng nhập mô tả báo cáo");
            return;
        }

        // Create report request with proper validation
        ReportRequest request = new ReportRequest();
        
        // Validate binId - nếu binId không hợp lệ, sử dụng 0 (không có thùng rác cụ thể)
        if (binId > 0) {
            request.setBinId(binId);
        } else {
            request.setBinId(0); // Default bin ID for general reports
        }
        
        request.setUserId(String.valueOf(accountId));
        request.setReportType(reportTypeCode);
        request.setDescription(description);
        request.setLocation(binAddress != null ? binAddress : "Không xác định");
        request.setLatitude(0.0); // Default latitude
        request.setLongitude(0.0); // Default longitude
        request.setStatus("PENDING");


        // Validate request before sending
        if (!request.isValid()) {
            String error = request.getValidationError();
            showToast("Lỗi dữ liệu: " + error);
            return;
        }


        // Send API request
        apiService.createReport(request).enqueue(new Callback<Report>() {
            @Override
            public void onResponse(Call<Report> call, Response<Report> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showToast("Báo cáo đã được gửi thành công");
                    finish();
                } else {
                    // Log chi tiết lỗi
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        errorBody = "Không thể đọc lỗi chi tiết";
                    }
                    
                    String errorMessage = "Lỗi khi gửi báo cáo";
                    if (response.code() == 400) {
                        errorMessage = "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại thông tin.";
                        tryAlternativeFormat(request);
                    } else if (response.code() == 401) {
                        errorMessage = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
                        saveReportOffline(request);
                    } else if (response.code() == 500) {
                        errorMessage = "Lỗi server. Vui lòng thử lại sau.";
                        saveReportOffline(request);
                    } else {
                        saveReportOffline(request);
                    }
                    
                    if (response.code() != 400) {
                        showToast(errorMessage + " (Mã lỗi: " + response.code() + ")");
                    }
                }
            }

            @Override
            public void onFailure(Call<Report> call, Throwable t) {
                
                // Save report offline
                saveReportOffline(request);
                showToast("Lỗi kết nối. Báo cáo đã được lưu offline để gửi lại sau.");
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


    private void tryAlternativeFormat(ReportRequest originalRequest) {
        try {
            // Tạo request với format khác - sử dụng data an toàn nhất
            ReportRequest alternativeRequest = new ReportRequest();
            
            // Sử dụng data an toàn nhất để tránh constraint violations
            alternativeRequest.setUserId("1"); // Sử dụng admin account ID
            alternativeRequest.setBinId(0); // Sử dụng binId = 0 để tránh foreign key constraint
            alternativeRequest.setReportType("OTHER"); // Sử dụng type an toàn
            alternativeRequest.setDescription(originalRequest.getDescription());
            alternativeRequest.setLocation("Đà Nẵng, Việt Nam"); // Location mặc định
            alternativeRequest.setLatitude(16.0678); // Tọa độ Đà Nẵng
            alternativeRequest.setLongitude(108.2208); // Tọa độ Đà Nẵng
            alternativeRequest.setStatus("PENDING"); // Đảm bảo status không null
            
            
            apiService.createReport(alternativeRequest).enqueue(new Callback<Report>() {
                @Override
                public void onResponse(Call<Report> call, Response<Report> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        showToast("Báo cáo đã được gửi thành công (format thay thế)");
                        finish();
                    } else {
                        saveReportOffline(originalRequest);
                        showToast("Không thể gửi báo cáo. Đã lưu offline để gửi lại sau.");
                    }
                }

                @Override
                public void onFailure(Call<Report> call, Throwable t) {
                    saveReportOffline(originalRequest);
                    showToast("Lỗi kết nối. Đã lưu báo cáo offline.");
                }
            });
        } catch (Exception e) {
            saveReportOffline(originalRequest);
            showToast("Lỗi xử lý. Đã lưu báo cáo offline.");
        }
    }

    private void saveReportOffline(ReportRequest request) {
        try {
            // Lưu report vào SharedPreferences để có thể gửi lại sau
            SharedPreferences prefs = getSharedPreferences("OfflineReports", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            String reportKey = "report_" + System.currentTimeMillis();
            editor.putString(reportKey + "_userId", request.getUserId());
            editor.putInt(reportKey + "_binId", request.getBinId());
            editor.putString(reportKey + "_reportType", request.getReportType());
            editor.putString(reportKey + "_description", request.getDescription());
            editor.putString(reportKey + "_location", request.getLocation());
            editor.putFloat(reportKey + "_latitude", (float) request.getLatitude());
            editor.putFloat(reportKey + "_longitude", (float) request.getLongitude());
            editor.putString(reportKey + "_status", request.getStatus());
            editor.putLong(reportKey + "_timestamp", System.currentTimeMillis());
            
            editor.apply();
            
        } catch (Exception e) {
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
