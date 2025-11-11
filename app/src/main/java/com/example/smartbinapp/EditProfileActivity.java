package com.example.smartbinapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.smartbinapp.model.Account;
import com.example.smartbinapp.model.Province;
import com.example.smartbinapp.model.Ward;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etFullName, etPhone, etAddress;
    private Spinner spnProvince, spnWard;
    private Button btnSaveProfile;
    private ImageView ivAvatarEdit;

    private ApiService apiService;
    private String userId;

    private List<Province> provinceList = new ArrayList<>();
    private List<Ward> wardList = new ArrayList<>();
    private ArrayAdapter<String> provinceAdapter;
    private ArrayAdapter<String> wardAdapter;

    private Integer selectedWardId = null;
    private Integer currentWardId = null;
    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int CAMERA_REQUEST = 101;
    private Uri selectedImageUri;
    private Uri cameraImageUri;
    private String uploadedImageUrl = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        ivAvatarEdit = findViewById(R.id.ivAvatarEdit);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        spnProvince = findViewById(R.id.spnProvince);
        spnWard = findViewById(R.id.spnWard);

        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // 🟢 Load dữ liệu người dùng
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        etFullName.setText(prefs.getString("userName", ""));
        etPhone.setText(prefs.getString("phone", ""));
        etAddress.setText(prefs.getString("address", ""));
        currentWardId = prefs.getInt("wardID", -1);
        String avatarUrl = prefs.getString("avatarUrl", null);

        Log.d("DEBUG", "🟢 currentWardId trong session = " + currentWardId);

        // 🟢 Load ảnh avatar
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.defaul_avarta)
                            .error(R.drawable.defaul_avarta)
                            .circleCrop())
                    .into(ivAvatarEdit);
        } else {
            ivAvatarEdit.setImageResource(R.drawable.defaul_avarta);
        }

        loadProvinces();

        btnSaveProfile.setOnClickListener(v -> updateProfile());
        ivAvatarEdit.setOnClickListener(v -> showImagePickerDialog());
    }

    private void loadProvinces() {
        apiService.getProvinces().enqueue(new Callback<List<Province>>() {
            @Override
            public void onResponse(Call<List<Province>> call, Response<List<Province>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    provinceList = response.body();
                    List<String> provinceNames = new ArrayList<>();
                    for (Province p : provinceList) provinceNames.add(p.getProvinceName());

                    provinceAdapter = new ArrayAdapter<>(EditProfileActivity.this,
                            android.R.layout.simple_spinner_dropdown_item, provinceNames);
                    spnProvince.setAdapter(provinceAdapter);

                    spnProvince.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                            int provinceId = provinceList.get(position).getProvinceId();
                            loadWards(provinceId);
                        }

                        @Override
                        public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                    });
                }
            }

            @Override
            public void onFailure(Call<List<Province>> call, Throwable t) {
                Toast.makeText(EditProfileActivity.this, "Lỗi tải tỉnh: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadWards(int provinceId) {
        apiService.getWards(provinceId).enqueue(new Callback<List<Ward>>() {
            @Override
            public void onResponse(Call<List<Ward>> call, Response<List<Ward>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    wardList = response.body();
                    List<String> wardNames = new ArrayList<>();
                    for (Ward w : wardList) wardNames.add(w.getWardName());

                    wardAdapter = new ArrayAdapter<>(EditProfileActivity.this,
                            android.R.layout.simple_spinner_dropdown_item, wardNames);
                    spnWard.setAdapter(wardAdapter);

                    // 🟢 Trường hợp không có currentWardId (đăng ký mới hoặc chưa chọn)
                    if (currentWardId == null || currentWardId <= 0) {
                        if (!wardList.isEmpty()) {
                            selectedWardId = wardList.get(0).getWardId();
                            spnWard.setSelection(0);
                            Log.d("DEBUG", "🟢 Mặc định chọn ward đầu tiên = " + selectedWardId);
                        }
                    } else {
                        // 🟢 Giữ lại ward cũ trong session
                        boolean found = false;
                        for (int i = 0; i < wardList.size(); i++) {
                            if (wardList.get(i).getWardId() == currentWardId) {
                                spnWard.setSelection(i);
                                selectedWardId = currentWardId;
                                found = true;
                                Log.d("DEBUG", "🟢 Giữ nguyên wardId hiện tại = " + currentWardId);
                                break;
                            }
                        }
                        if (!found && !wardList.isEmpty()) {
                            // Nếu không tìm thấy ward cũ, chọn ward đầu tiên
                            selectedWardId = wardList.get(0).getWardId();
                            spnWard.setSelection(0);
                            Log.d("DEBUG", "🟢 Không tìm thấy ward cũ, chọn mặc định = " + selectedWardId);
                        }
                    }

                    // 🟢 Luôn cập nhật selectedWardId khi người dùng thay đổi
                    spnWard.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                            if (position >= 0 && position < wardList.size()) {
                                selectedWardId = wardList.get(position).getWardId();
                                Log.d("DEBUG", "🟢 Người dùng chọn wardId = " + selectedWardId);
                            }
                        }

                        @Override
                        public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                    });

                    // 🟢 Debug check: đảm bảo wardId != 0
                    Log.d("DEBUG", "✅ WardList size = " + wardList.size() + ", selectedWardId = " + selectedWardId);
                } else {
                    Toast.makeText(EditProfileActivity.this, "⚠️ Không có dữ liệu phường!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Ward>> call, Throwable t) {
                Toast.makeText(EditProfileActivity.this, "Lỗi tải phường: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void showImagePickerDialog() {
        String[] options = {"📷 Chụp ảnh bằng camera", "🖼️ Chọn ảnh từ thư viện"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Chọn ảnh đại diện")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndOpen(); // ✅ Sửa tại đây
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền sử dụng camera!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        File photoFile;
        try {
            photoFile = File.createTempFile(
                    "avatar_" + System.currentTimeMillis(),
                    ".jpg",
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            );
        } catch (IOException e) {
            Toast.makeText(this, "Lỗi tạo file ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            cameraImageUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".provider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);

            // ✅ Cấp quyền tạm cho camera app
            grantUriPermission(getPackageName(), cameraImageUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                startActivityForResult(intent, CAMERA_REQUEST);
            } catch (Exception e) {
                Toast.makeText(this, "⚠️ Không mở được camera!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                selectedImageUri = data.getData();
                ivAvatarEdit.setImageURI(selectedImageUri);
            } else if (requestCode == CAMERA_REQUEST && cameraImageUri != null) {
                selectedImageUri = cameraImageUri;
                ivAvatarEdit.setImageURI(selectedImageUri);
            }
        }
    }

    private void uploadAvatarToFirebase(Runnable onSuccess) {
        if (selectedImageUri == null) {
            onSuccess.run();
            return;
        }

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("avatars/" + userId + "_" + System.currentTimeMillis() + ".jpg");

        UploadTask uploadTask = storageRef.putFile(selectedImageUri);
        uploadTask.addOnSuccessListener(taskSnapshot ->
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    uploadedImageUrl = uri.toString();
                    Log.d("FIREBASE", "Ảnh đã upload: " + uploadedImageUrl);
                    onSuccess.run();
                })
        ).addOnFailureListener(e ->
                Toast.makeText(this, "Upload ảnh thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void updateProfile() {
        if (userId == null) {
            Toast.makeText(this, "Không xác định được tài khoản!", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (fullName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập họ tên!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedWardId == null || selectedWardId <= 0) {
            Toast.makeText(this, "⚠️ Vui lòng chọn phường hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null)
            uploadAvatarToFirebase(() -> sendUpdateRequest(fullName, phone, address));
        else
            sendUpdateRequest(fullName, phone, address);
    }

    private void sendUpdateRequest(String fullName, String phone, String address) {
        Account account = new Account();
        account.setAccountId(Integer.parseInt(userId));
        account.setFullName(fullName);
        account.setPhone(phone);
        account.setAddressDetail(address);
        account.setWardId(selectedWardId);
        if (uploadedImageUrl != null) account.setAvatarUrl(uploadedImageUrl);

        apiService.updateAccount(userId, account).enqueue(new Callback<Account>() {
            @Override
            public void onResponse(Call<Account> call, Response<Account> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, "✅ Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(EditProfileActivity.this, ProfileActivity.class));
                    finish();
                } else {
                    Toast.makeText(EditProfileActivity.this, "Lỗi cập nhật!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Account> call, Throwable t) {
                Toast.makeText(EditProfileActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
