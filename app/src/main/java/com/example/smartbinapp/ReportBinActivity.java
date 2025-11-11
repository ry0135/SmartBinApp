package com.example.smartbinapp;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbinapp.adapter.ImageAdapter;
import com.example.smartbinapp.model.ReportRequest;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

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
    private LinearLayout btnHome, btnReport, btnShowTask, btnAccount, bottomNavigation;
    private ProgressBar progressBar;

    private ApiService apiService;
    private ImageAdapter imageAdapter;
    private List<Uri> imageUris = new ArrayList<>();
    private List<String> uploadedImageUrls = new ArrayList<>();

    private int binId;
    private String binCode, binAddress;
    private Integer accountId;
    private boolean isRequestingPermission = false;
    private Toast currentToast;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_bin);

        initializeViews();
        setupToolbar();
        setupDropdown();
        setupRecyclerView();
        setupClickListeners();
//        setupBottomNavigation();
        startEntranceAnimations();

        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        setupImagePickerLauncher();
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
        progressBar = findViewById(R.id.progressBar);

        btnHome = findViewById(R.id.btn_home);
        btnReport = findViewById(R.id.btn_report);
        btnShowTask = findViewById(R.id.btn_showtask);
        btnAccount = findViewById(R.id.btn_account);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        accountId = prefs.getInt("accountId", 1);
        int savedRole = prefs.getInt("role", 0);
        if (savedRole == 4) {
            btnShowTask.setVisibility(View.GONE);
            Log.d("RoleCheck", "Đã ẩn nút Nhiệm vụ vì người dùng là citizen");
        }
        binId = getIntent().getIntExtra("bin_id", -1);
        binCode = getIntent().getStringExtra("bin_code");
        binAddress = getIntent().getStringExtra("bin_address");

        tvBinCode.setText("Mã thùng: " + binCode);
        tvBinAddress.setText("Địa chỉ: " + binAddress);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, REPORT_TYPES);
        actvReportType.setAdapter(adapter);
    }

    private void setupRecyclerView() {
        imageAdapter = new ImageAdapter(this);
        rvImages.setAdapter(imageAdapter);
    }

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.getClipData() != null) {
                            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                                Uri uri = data.getClipData().getItemAt(i).getUri();
                                imageAdapter.addImage(uri);
                            }
                            showToast("Đã thêm " + data.getClipData().getItemCount() + " ảnh");
                        } else if (data.getData() != null) {
                            imageAdapter.addImage(data.getData());
                            showToast("Đã thêm 1 ảnh");
                        }
                    }
                });
    }

    private void setupClickListeners() {
        btnAddImage.setOnClickListener(v -> checkStoragePermissionAndPickImage());
        btnSubmit.setOnClickListener(v -> {
            if (validateInputs()) uploadImagesToFirebase();
        });
    }

//    private void setupBottomNavigation() {
//        btnHome.setOnClickListener(v -> {
//            startActivity(new Intent(this, HomeActivity.class));
//            finish();
//        });
//        btnReport.setOnClickListener(v -> {
//            startActivity(new Intent(this, ReportsListActivity.class));
//        });
//        btnShowTask.setOnClickListener(v -> {
//            startActivity(new Intent(this, TaskSummaryActivity.class));
//        });
//        btnAccount.setOnClickListener(v -> {
//            startActivity(new Intent(this, ProfileActivity.class));
//        });
//    }

    private void startEntranceAnimations() {
        ObjectAnimator headerAnim = ObjectAnimator.ofFloat(toolbar, "translationY", -100f, 0f);
        headerAnim.setDuration(800);
        headerAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        headerAnim.start();

        ObjectAnimator bottomAnim = ObjectAnimator.ofFloat(bottomNavigation, "translationY", 100f, 0f);
        bottomAnim.setDuration(800);
        bottomAnim.setStartDelay(200);
        bottomAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        bottomAnim.start();
    }

    private boolean validateInputs() {
        boolean valid = true;
        if (actvReportType.getText().toString().trim().isEmpty()) {
            tilReportType.setError("Vui lòng chọn loại báo cáo");
            valid = false;
        } else tilReportType.setError(null);

        if (etDescription.getText().toString().trim().isEmpty()) {
            tilDescription.setError("Vui lòng nhập mô tả");
            valid = false;
        } else tilDescription.setError(null);

        return valid;
    }

    private void checkStoragePermissionAndPickImage() {
        if (isRequestingPermission) return;
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                isRequestingPermission = true;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        STORAGE_PERMISSION_REQUEST_CODE);
            } else openImagePicker();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                isRequestingPermission = true;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_REQUEST_CODE);
            } else openImagePicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        imagePickerLauncher.launch(Intent.createChooser(intent, "Chọn ảnh báo cáo"));
    }

    /** 🔥 Upload ảnh lên Firebase Storage */
    private void uploadImagesToFirebase() {
        if (imageAdapter.getImageUris().isEmpty()) {
            createReport();
            return;
        }

        uploadedImageUrls.clear();
        progressBar.setVisibility(ProgressBar.VISIBLE);
        uploadNextFirebase(0);
    }

    private void uploadNextFirebase(int index) {
        if (index >= imageAdapter.getImageUris().size()) {
            progressBar.setVisibility(ProgressBar.GONE);
            createReport();
            return;
        }

        Uri uri = imageAdapter.getImageUris().get(index);
        String fileName = "reports/" + accountId + "_" + System.currentTimeMillis() + "_" + index + ".jpg";

        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(fileName);
        UploadTask uploadTask = storageRef.putFile(uri);

        uploadTask.addOnSuccessListener(taskSnapshot ->
                storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    String imageUrl = downloadUri.toString();
                    uploadedImageUrls.add(imageUrl);
                    Log.d("FirebaseUpload", "✅ Uploaded: " + imageUrl);
                    uploadNextFirebase(index + 1);
                })
        ).addOnFailureListener(e -> {
            showToast("❌ Lỗi tải ảnh: " + e.getMessage());
            uploadNextFirebase(index + 1);
        });
    }

    /** 📤 Gửi dữ liệu báo cáo kèm URL ảnh Firebase lên backend */
    private void createReport() {
        ReportRequest req = new ReportRequest();
        req.setBinId(binId);
        req.setAccountId(accountId);
        req.setReportType("OTHER");
        req.setDescription(etDescription.getText().toString().trim());
        req.setLocation(binAddress);
        req.setStatus("PENDING");
        req.setImages(uploadedImageUrls);

        apiService.createReport(req).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                progressBar.setVisibility(ProgressBar.GONE);
                if (response.isSuccessful()) {
                    showToast("✅ Báo cáo đã được gửi thành công!");
                    finish();
                } else showToast("⚠️ Gửi thất bại: " + response.code());
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                progressBar.setVisibility(ProgressBar.GONE);
                showToast("🚫 Không thể gửi báo cáo: " + t.getMessage());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        isRequestingPermission = false;
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) openImagePicker();
        else showToast("Cần quyền truy cập để chọn hình ảnh.");
    }

    @Override
    public void onImageClick(int position) {
        imageAdapter.removeImage(position);
    }

    private void showToast(String msg) {
        if (currentToast != null) currentToast.cancel();
        currentToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        currentToast.show();
    }
}
