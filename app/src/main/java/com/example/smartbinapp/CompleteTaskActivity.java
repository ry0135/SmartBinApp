package com.example.smartbinapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.example.smartbinapp.model.ApiMessage;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CompleteTaskActivity extends AppCompatActivity {

    private int taskId;
    private double binLat, binLng;
    private String binCode;
    private Uri photoUri;
    private File tempPhotoFile;

    private ImageView ivSuccess, ivPreview;
    private CardView cardImagePreview, cardInstruction, cardSuccess;
    private MaterialButton btnCapture, btnConfirm, btnRetake, btnViewFull;
    private TextView tvBinCode, tvLocation, tvStep3Status;
    private FusedLocationProviderClient fusedClient;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    showImagePreview();
                    updateProgressStep3Completed();
                } else {
                    Toast.makeText(this, "Đã hủy chụp ảnh", Toast.LENGTH_SHORT).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_task);

        initializeViews();
        getIntentData();
        setupClickListeners();

        // Hiển thị instruction section ban đầu
        showInstructionSection();
    }

    private void initializeViews() {
        // Main views
        ivSuccess = findViewById(R.id.ivSuccess);
        ivPreview = findViewById(R.id.ivPreview);
        cardImagePreview = findViewById(R.id.cardImagePreview);
        cardInstruction = findViewById(R.id.cardInstruction);
        cardSuccess = findViewById(R.id.cardSuccess);

        // Buttons - CHỈ KHỞI TẠO NHỮNG BUTTON CÓ TRONG LAYOUT
        btnCapture = findViewById(R.id.btnCapture);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnRetake = findViewById(R.id.btnRetake);
        btnViewFull = findViewById(R.id.btnViewFull);

        // TextViews
        tvBinCode = findViewById(R.id.tvBinCode);
        tvLocation = findViewById(R.id.tvLocation);
        tvStep3Status = findViewById(R.id.tvStep3Status);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void getIntentData() {
        taskId = getIntent().getIntExtra("taskId", -1);
        binCode = getIntent().getStringExtra("binCode");
        binLat = getIntent().getDoubleExtra("binLat", 0);
        binLng = getIntent().getDoubleExtra("binLng", 0);

        // Update UI with bin data
        if (tvBinCode != null) {
            tvBinCode.setText(binCode);
        }
        if (tvLocation != null) {
            tvLocation.setText(String.format(Locale.getDefault(), "%.6f, %.6f", binLat, binLng));
        }
    }

    private void setupClickListeners() {
        // CHỈ GÁN CLICK LISTENER CHO NHỮNG BUTTON TỒN TẠI
        if (btnCapture != null) {
            btnCapture.setOnClickListener(v -> checkCameraPermission());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> confirmCompletion());
            btnConfirm.setEnabled(false); // Disable confirm button initially
        }

        if (btnRetake != null) {
            btnRetake.setOnClickListener(v -> retakePhoto());
        }

        if (btnViewFull != null) {
            btnViewFull.setOnClickListener(v -> viewFullScreenImage());
        }

        // Close preview button - KIỂM TRA NULL
        ImageButton btnClosePreview = findViewById(R.id.btnClosePreview);
        if (btnClosePreview != null) {
            btnClosePreview.setOnClickListener(v -> hideImagePreview());
        }

        // Copy location button - KIỂM TRA NULL
        ImageButton btnCopyLocation = findViewById(R.id.btnCopyLocation);
        if (btnCopyLocation != null) {
            btnCopyLocation.setOnClickListener(v -> copyLocationToClipboard());
        }
    }

    private void showInstructionSection() {
        if (cardInstruction != null) {
            cardInstruction.setVisibility(View.VISIBLE);
        }
        if (cardSuccess != null) {
            cardSuccess.setVisibility(View.GONE);
        }
    }

    private void showSuccessSection() {
        if (cardInstruction != null) {
            cardInstruction.setVisibility(View.GONE);
        }
        if (cardSuccess != null) {
            cardSuccess.setVisibility(View.VISIBLE);
        }

        // Animation cho success icon
        animateSuccessIcon();
    }

    private void animateSuccessIcon() {
        if (ivSuccess != null) {
            ivSuccess.setScaleX(0);
            ivSuccess.setScaleY(0);

            new Handler().postDelayed(() -> {
                ivSuccess.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .rotation(360f)
                        .setDuration(800)
                        .setInterpolator(new OvershootInterpolator(1.2f))
                        .start();
            }, 300);
        }
    }

    private void checkCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Cần quyền camera để chụp ảnh", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                confirmCompletion(); // Retry after location permission granted
            } else {
                Toast.makeText(this, "Cần quyền vị trí để xác nhận hoàn thành", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "PROOF_" + timeStamp + "_";

            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            tempPhotoFile = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );

            photoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    tempPhotoFile
            );

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cameraLauncher.launch(intent);

        } catch (IOException e) {
            Toast.makeText(this, "Lỗi tạo file ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showImagePreview() {
        if (photoUri != null) {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(photoUri));
                if (ivPreview != null) {
                    ivPreview.setImageBitmap(bitmap);
                }

                // Show preview card with animation
                if (cardImagePreview != null) {
                    cardImagePreview.setVisibility(View.VISIBLE);
                    cardImagePreview.setAlpha(0f);
                    cardImagePreview.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start();
                }

                // Enable confirm button
                if (btnConfirm != null) {
                    btnConfirm.setEnabled(true);
                }

            } catch (IOException e) {
                Toast.makeText(this, "Lỗi load ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void hideImagePreview() {
        if (cardImagePreview != null) {
            cardImagePreview.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> cardImagePreview.setVisibility(View.GONE))
                    .start();
        }
    }

    private void retakePhoto() {
        hideImagePreview();
        openCamera();
    }

    private void viewFullScreenImage() {
        if (photoUri != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(photoUri, "image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }
    }

    private void copyLocationToClipboard() {
        if (tvLocation != null) {
            String location = tvLocation.getText().toString();
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Location", location);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Đã sao chép vị trí", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProgressStep3Completed() {
        if (tvStep3Status != null) {
            tvStep3Status.setText("Đã hoàn thành");
            tvStep3Status.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));
        }
    }

    private void confirmCompletion() {
        if (tempPhotoFile == null || !tempPhotoFile.exists()) {
            Toast.makeText(this, "Vui lòng chụp ảnh minh chứng trước", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(this, "Không lấy được vị trí GPS. Vui lòng bật GPS và thử lại.", Toast.LENGTH_LONG).show();
                return;
            }

            double currentLat = location.getLatitude();
            double currentLng = location.getLongitude();

            // Check distance to bin (50m threshold)
            float[] distance = new float[1];
            android.location.Location.distanceBetween(currentLat, currentLng, binLat, binLng, distance);

            if (distance[0] > 500) {
                Toast.makeText(this,
                        "Bạn đang cách thùng quá xa (" + (int) distance[0] + "m). Vui lòng đến gần hơn.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            uploadImageToServer(currentLat, currentLng);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi lấy vị trí: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void uploadImageToServer(double lat, double lng) {
        if (btnConfirm != null) {
            btnConfirm.setEnabled(false);
            btnConfirm.setText("Đang xử lý...");
        }

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        try {
            // Log thông tin request
            Log.d("API_UPLOAD", "Uploading - Task: " + taskId +
                    ", Lat: " + lat + ", Lng: " + lng +
                    ", File: " + tempPhotoFile.getName());

            // Prepare data
            RequestBody taskIdBody = RequestBody.create(String.valueOf(taskId), MediaType.parse("text/plain"));
            RequestBody latBody = RequestBody.create(String.valueOf(lat), MediaType.parse("text/plain"));
            RequestBody lngBody = RequestBody.create(String.valueOf(lng), MediaType.parse("text/plain"));

            RequestBody fileBody = RequestBody.create(tempPhotoFile, MediaType.parse("image/jpeg"));
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", tempPhotoFile.getName(), fileBody);

            // Call API
            Call<ApiMessage> call = apiService.completeTaskWithImage(taskIdBody, latBody, lngBody, imagePart);

            call.enqueue(new Callback<ApiMessage>() {
                @Override
                public void onResponse(Call<ApiMessage> call, Response<ApiMessage> response) {
                    if (btnConfirm != null) {
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText("Xác nhận");
                    }

                    if (response.isSuccessful() && response.body() != null) {
                        Log.d("API_UPLOAD", "Success: " + response.body().getMessage());
                        showSuccessAndFinish();
                    } else {
                        String errorMsg = "Lỗi Server: " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                errorMsg = response.errorBody().string();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.e("API_UPLOAD", "Error: " + errorMsg);
                        Toast.makeText(CompleteTaskActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiMessage> call, Throwable t) {
                    if (btnConfirm != null) {
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText("Xác nhận");
                    }
                    Log.e("API_UPLOAD", "Failure: " + t.getMessage());
                    Toast.makeText(CompleteTaskActivity.this,
                            "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            if (btnConfirm != null) {
                btnConfirm.setEnabled(true);
                btnConfirm.setText("Xác nhận");
            }
            Log.e("API_UPLOAD", "Exception: " + e.getMessage());
            Toast.makeText(this, "Lỗi chuẩn bị dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showSuccessAndFinish() {
        // Hiển thị hiệu ứng thành công
        showSuccessSection();
        Toast.makeText(this, "✅ Hoàn thành nhiệm vụ thành công!", Toast.LENGTH_LONG).show();

        new Handler().postDelayed(() -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("taskId", taskId);
            resultIntent.putExtra("status", "COMPLETED");
            setResult(RESULT_OK, resultIntent);
            finish();
        }, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up temp file if needed
        if (tempPhotoFile != null && tempPhotoFile.exists()) {
            // tempPhotoFile.delete(); // Uncomment if you want to delete temp files immediately
        }
    }
}