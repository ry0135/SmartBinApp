package com.example.smartbinapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.example.smartbinapp.model.Bin;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import vn.vietmap.vietmapsdk.annotations.IconFactory;
import vn.vietmap.vietmapsdk.annotations.MarkerOptions;
import vn.vietmap.vietmapsdk.camera.CameraUpdateFactory;
import vn.vietmap.vietmapsdk.geometry.LatLng;
import vn.vietmap.vietmapsdk.location.LocationComponent;
import vn.vietmap.vietmapsdk.location.LocationComponentActivationOptions;
import vn.vietmap.vietmapsdk.location.modes.CameraMode;
import vn.vietmap.vietmapsdk.location.modes.RenderMode;
import vn.vietmap.vietmapsdk.maps.MapView;
import vn.vietmap.vietmapsdk.maps.OnMapReadyCallback;
import vn.vietmap.vietmapsdk.maps.Style;
import vn.vietmap.vietmapsdk.maps.VietMapGL;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NearbyBinsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String VIETMAP_API_KEY = "ecdbd35460b2d399e18592e6264186757aaaddd8755b774c";

    private MapView mapView;
    private VietMapGL vietmapGL;
    private FusedLocationProviderClient fusedLocationClient;
    private ApiService apiService;

    // UI components for reporting
    private CardView cardBinInfo;
    private TextView tvBinCode, tvBinAddress, tvFillLevel;
    private ProgressBar progressFill;
    private Button btnReport;

    // Currently selected/nearest bin
    private Bin selectedBin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_bins);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        // Initialize bottom card views
        cardBinInfo = findViewById(R.id.cardBinInfo);
        tvBinCode = findViewById(R.id.tvBinCode);
        tvBinAddress = findViewById(R.id.tvBinAddress);
        tvFillLevel = findViewById(R.id.tvFillLevel);
        progressFill = findViewById(R.id.progressFill);
        btnReport = findViewById(R.id.btnReport);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        try {
            if (mapView != null) {
                mapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(VietMapGL map) {
                        try {
                            vietmapGL = map;

                            vietmapGL.setStyle(
                                    new Style.Builder()
                                            .fromUri("https://maps.vietmap.vn/api/maps/light/styles.json?apikey=" + VIETMAP_API_KEY),
                                    style -> {
                                        try {
                                            // Delay để map load xong hẳn
                                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                                try {
                                                    if (ActivityCompat.checkSelfPermission(
                                                            NearbyBinsActivity.this,
                                                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                                                        enableLocationComponent();

                                                        // Set cứng tọa độ Hội An để test
                                                        double latitude = 15.8801;  // Hội An
                                                        double longitude = 108.338; // Hội An
                                                        
                                                        if (vietmapGL != null) {
                                                            LatLng hoiAnLatLng = new LatLng(latitude, longitude);
                                                            vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(hoiAnLatLng, 15));
                                                            Toast.makeText(NearbyBinsActivity.this, "Đang tìm thùng rác gần Hội An...", Toast.LENGTH_SHORT).show();
                                                            fetchNearbyBins(latitude, longitude);
                                                        }
                                                    } else {
                                                        // Xin quyền vị trí
                                                        ActivityCompat.requestPermissions(
                                                                NearbyBinsActivity.this,
                                                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                                                LOCATION_PERMISSION_REQUEST_CODE
                                                        );
                                                    }
                                                } catch (Exception e) {
                                                    Log.e("NearbyBinsActivity", "Map setup failed: " + e.getMessage());
                                                }
                                            }, 1000);
                                        } catch (Exception e) {
                                            Log.e("NearbyBinsActivity", "Style loading failed: " + e.getMessage());
                                        }
                                    });
                        } catch (Exception e) {
                            Log.e("NearbyBinsActivity", "Map ready failed: " + e.getMessage());
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e("NearbyBinsActivity", "Map async loading failed: " + e.getMessage());
        }
    }

    private void updateBinInfoCard(Bin bin) {
        if (cardBinInfo == null) return;

        tvBinCode.setText(safe(bin.getBinCode()));

        StringBuilder addressBuilder = new StringBuilder();
        if (bin.getStreet() != null) addressBuilder.append(bin.getStreet());
        if (bin.getWardName() != null) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(bin.getWardName());
        }
        if (bin.getProvinceName() != null) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(bin.getProvinceName());
        }
        tvBinAddress.setText(addressBuilder.toString());

        double capacity = bin.getCapacity() > 0 ? bin.getCapacity() : 1;
        int percent = (int) ((bin.getCurrentFill() / capacity) * 100);
        tvFillLevel.setText(String.valueOf(percent) + "%");
        if (progressFill != null) progressFill.setProgress(percent);

        // Show card
        cardBinInfo.setVisibility(View.VISIBLE);

        // Wire up report button
        if (btnReport != null) {
            btnReport.setOnClickListener(v -> {
                if (selectedBin != null) {
                    Intent intent = new Intent(NearbyBinsActivity.this, ReportBinActivity.class);
                    intent.putExtra("bin_id", selectedBin.getBinId());
                    intent.putExtra("bin_code", selectedBin.getBinCode());
                    intent.putExtra("bin_address", tvBinAddress.getText().toString());
                    startActivity(intent);
                } else {
                    Toast.makeText(NearbyBinsActivity.this, "Chưa chọn thùng rác để báo cáo", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String safe(String s) { return s == null ? "" : s; }

    private void enableLocationComponent() {
        if (vietmapGL != null) {
            // Tạm thời để trống, bạn có thể enable LocationComponent nếu cần
            Toast.makeText(this, "Đã bật định vị trên bản đồ", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchNearbyBins(double latitude, double longitude) {
        // Sử dụng endpoint mới cho Hội An
        apiService.getNearbyBinsHoiAn().enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call, retrofit2.Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        Log.d("NearbyBinsActivity", "Nearby API Response: " + responseString);
                        
                        // Parse the response manually
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        com.google.gson.JsonObject jsonObject = gson.fromJson(responseString, com.google.gson.JsonObject.class);
                        
                        if (jsonObject.has("data")) {
                            com.google.gson.JsonElement dataElement = jsonObject.get("data");
                            if (dataElement.isJsonArray()) {
                                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Bin>>(){}.getType();
                                List<Bin> bins = gson.fromJson(dataElement, listType);
                                if (bins != null && !bins.isEmpty()) {
                                    // Chỉ lấy thùng rác gần nhất (đầu tiên trong danh sách)
                                    Bin nearestBin = bins.get(0);
                                    selectedBin = nearestBin; // Set selectedBin trước khi sử dụng
                                    if (vietmapGL != null) {
                                        // Clear existing markers trước
                                        vietmapGL.clear();
                                        
                                        // Thêm marker cho thùng rác gần nhất
                                        vietmapGL.addMarker(new MarkerOptions()
                                                .position(new LatLng(nearestBin.getLatitude(), nearestBin.getLongitude()))
                                                .title("Thùng rác gần nhất: " + nearestBin.getBinCode())
                                                .snippet("Vị trí: " + nearestBin.getLatitude() + ", " + nearestBin.getLongitude()));
                                        
                                        // Zoom vào thùng rác gần nhất
                                        vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                                new LatLng(nearestBin.getLatitude(), nearestBin.getLongitude()), 16));
                                    }
                                    
                                    // Cập nhật card và bật nút Report
                                    updateBinInfoCard(nearestBin);
                                    return;
                                }
                            }
                        }
                        
                        Toast.makeText(NearbyBinsActivity.this, "Không có thùng rác gần đây. Đang tải tất cả thùng rác...", Toast.LENGTH_SHORT).show();
                        loadAllBins();
                        
                    } catch (Exception e) {
                        Log.e("NearbyBinsActivity", "Parse nearby response failed: " + e.getMessage());
                        loadAllBins();
                    }
                } else {
                    Toast.makeText(NearbyBinsActivity.this, "Không có thùng rác gần đây. Đang tải tất cả thùng rác...", Toast.LENGTH_SHORT).show();
                    loadAllBins();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                Toast.makeText(NearbyBinsActivity.this, "Lỗi API: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                loadAllBins();
            }
        });
    }

    private void loadAllBins() {
        apiService.getAllBins().enqueue(new Callback<List<Bin>>() {
            @Override
            public void onResponse(Call<List<Bin>> call, Response<List<Bin>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // Chỉ lấy thùng rác đầu tiên (gần nhất) từ tất cả thùng rác
                    Bin nearestBin = response.body().get(0);
                    selectedBin = nearestBin; // Set selectedBin trước khi sử dụng
                    if (vietmapGL != null) {
                        // Clear existing markers trước
                        vietmapGL.clear();
                        
                        // Thêm marker cho thùng rác gần nhất
                        vietmapGL.addMarker(new MarkerOptions()
                                .position(new LatLng(nearestBin.getLatitude(), nearestBin.getLongitude()))
                                .title("Thùng rác gần nhất: " + nearestBin.getBinCode())
                                .snippet("Vị trí: " + nearestBin.getLatitude() + ", " + nearestBin.getLongitude()));
                        
                        // Zoom vào thùng rác gần nhất
                        vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(nearestBin.getLatitude(), nearestBin.getLongitude()), 16));
                    }
                    
                    updateBinInfoCard(nearestBin);
                } else {
                    Toast.makeText(NearbyBinsActivity.this, "Không thể tải dữ liệu thùng rác", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Bin>> call, Throwable t) {
                Toast.makeText(NearbyBinsActivity.this, "Lỗi tải dữ liệu: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Xử lý xin quyền vị trí
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Quyền vị trí đã được cấp", Toast.LENGTH_SHORT).show();
                recreate(); // load lại Activity để map cập nhật quyền
            } else {
                Toast.makeText(this, "Ứng dụng cần quyền truy cập vị trí", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Lifecycle MapView
    @Override
    protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override
    protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override
    protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override
    protected void onStop() { super.onStop(); mapView.onStop(); }
    //    protected void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
    @Override
    protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
