package com.example.smartbinapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.smartbinapp.model.Bin;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import vn.vietmap.vietmapsdk.Vietmap;
import vn.vietmap.vietmapsdk.camera.CameraPosition;
import vn.vietmap.vietmapsdk.camera.CameraUpdateFactory;
import vn.vietmap.vietmapsdk.geometry.LatLng;
import vn.vietmap.vietmapsdk.location.LocationComponent;
import vn.vietmap.vietmapsdk.location.LocationComponentActivationOptions;
import vn.vietmap.vietmapsdk.location.LocationComponentOptions;
import vn.vietmap.vietmapsdk.location.modes.CameraMode;
import vn.vietmap.vietmapsdk.location.modes.RenderMode;
import vn.vietmap.vietmapsdk.maps.MapView;
import vn.vietmap.vietmapsdk.maps.VietMapGL;
import vn.vietmap.vietmapsdk.maps.OnMapReadyCallback;
import vn.vietmap.vietmapsdk.maps.Style;

public class HomeActivityCitizen extends AppCompatActivity implements OnMapReadyCallback {

    private static final String VIETMAP_API_KEY = "ecdbd35460b2d399e18592e6264186757aaaddd8755b774c";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private MapView vmMapView;
    private VietMapGL vietmapGL;
    private ImageView ivMenu;
    private FloatingActionButton fabReport, fabMenu;
    private LinearLayout btnHome, btnNearby, btnMyReports, btnAccount;
    
    private ApiService apiService;
    private List<Bin> allBins;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Khởi tạo SDK VietMap TRƯỚC khi inflate MapView để tránh InflateException
        try {
            Vietmap.getInstance(this);
        } catch (Exception e) {
            android.util.Log.e("HomeActivityCitizen", "VietMap init failed: " + e.getMessage());
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_citizen);

        // Initialize API service
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        initializeViews();
        setupClickListeners();
        setupMap();
        
        // Load tất cả thùng rác từ database
        loadAllBins();
    }

    private void initializeViews() {
        vmMapView = findViewById(R.id.vmMapView);
        ivMenu = findViewById(R.id.iv_menu);
        fabReport = findViewById(R.id.fab_report);
        fabMenu = findViewById(R.id.fab_menu);
        btnHome = findViewById(R.id.btn_home);
        btnNearby = findViewById(R.id.btn_nearby);
        btnMyReports = findViewById(R.id.btn_my_reports);
        btnAccount = findViewById(R.id.btn_account);
    }

    private void setupClickListeners() {
        // Menu button
        ivMenu.setOnClickListener(v -> showMenu());

        // Floating Action Buttons
        fabReport.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivityCitizen.this, ReportBinActivity.class);
            startActivity(intent);
        });

        fabMenu.setOnClickListener(v -> showMenu());

        // Bottom Navigation

        btnNearby.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivityCitizen.this, NearbyBinsActivity.class);
            startActivity(intent);
        });

        btnMyReports.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivityCitizen.this, ReportsListActivity.class);
            startActivity(intent);
        });

        btnAccount.setOnClickListener(v -> showAccountMenu());
    }

    private void setupMap() {
        if (vmMapView != null) {
            vmMapView.onCreate(null);
            vmMapView.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(VietMapGL map) {
        vietmapGL = map;
        
        vietmapGL.setStyle(
            new Style.Builder()
                .fromUri("https://maps.vietmap.vn/api/maps/light/styles.json?apikey=" + VIETMAP_API_KEY),
            style -> {
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    enableLocationComponent();
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                }
            }
        );
    }
    private void enableLocationComponent() {
        LocationComponent locationComponent = vietmapGL.getLocationComponent();

        LocationComponentOptions customOptions = LocationComponentOptions.builder(this)
                .foregroundDrawable(R.drawable.ic_my_location)
                .backgroundDrawable(R.drawable.ic_my_location)
                .build();

        LocationComponentActivationOptions options =
                LocationComponentActivationOptions.builder(this, vietmapGL.getStyle())
                        .useDefaultLocationEngine(true) // ✅ Cho SDK tự dùng LocationEngine bên trong
                        .locationComponentOptions(customOptions)
                        .build();

        locationComponent.activateLocationComponent(options);
        locationComponent.setCameraMode(CameraMode.TRACKING);
        locationComponent.setRenderMode(RenderMode.NORMAL);

        // Test log vị trí
        if (locationComponent.getLastKnownLocation() != null) {
            double lat = locationComponent.getLastKnownLocation().getLatitude();
            double lng = locationComponent.getLastKnownLocation().getLongitude();

            vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(lat, lng), 16));
        } else {
            Log.d("DEBUG_LOCATION", "Không có GPS → dùng fallback Đà Nẵng");

            // Tạo location giả
            Location fakeLocation = new Location("fallback");
            fakeLocation.setLatitude(16.0544);
            fakeLocation.setLongitude(108.2022);

            // Gán cho LocationComponent
            locationComponent.forceLocationUpdate(fakeLocation);

            // Camera cũng bay đến đó
            vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(16.0544, 108.2022), 16));
        }
    }

    private void loadAllBins() {
        Log.d("HomeActivityCitizen", "=== BẮT ĐẦU LOAD TẤT CẢ THÙNG RÁC ===");
        
        apiService.getAllBins().enqueue(new Callback<List<Bin>>() {
            @Override
            public void onResponse(Call<List<Bin>> call, Response<List<Bin>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allBins = response.body();
                    Log.d("HomeActivityCitizen", "Load thành công: " + allBins.size() + " thùng rác");
                    
                    // Display bins trên map hoặc UI
                    displayBinsOnMap(allBins);
                } else {
                    Log.e("HomeActivityCitizen", "Lỗi load bins: " + response.code());
                    Toast.makeText(HomeActivityCitizen.this, "Không thể tải danh sách thùng rác", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Bin>> call, Throwable t) {
                Log.e("HomeActivityCitizen", "Lỗi kết nối khi load bins: " + t.getMessage());
                Toast.makeText(HomeActivityCitizen.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayBinsOnMap(List<Bin> bins) {
        if (bins == null || bins.isEmpty()) {
            Log.d("HomeActivityCitizen", "Không có thùng rác nào để hiển thị");
            return;
        }

        Log.d("HomeActivityCitizen", "Hiển thị " + bins.size() + " thùng rác trên map");
        
        // TODO: Thêm markers cho mỗi thùng rác trên map
        // Ví dụ:
        for (Bin bin : bins) {
            Log.d("HomeActivityCitizen", "Thùng rác: " + bin.getBinCode() + 
                  " - Lat: " + bin.getLatitude() + 
                  " - Lng: " + bin.getLongitude() +
                  " - Status: " + bin.getStatus());
        }
    }

    private void showMenu() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Menu Citizen");
        
        String[] options = {
            "Xem thùng rác gần nhất",
            "Báo cáo của tôi", 
            "Đánh giá dịch vụ",
            "Thông tin tài khoản",
            "Đăng xuất"
        };

        builder.setItems(options, (dialog, which) -> {
            Intent intent = null;
            switch (which) {
                case 0: // Xem thùng rác gần nhất
                    intent = new Intent(HomeActivityCitizen.this, NearbyBinsActivity.class);
                    break;
                case 1: // Báo cáo của tôi
                    intent = new Intent(HomeActivityCitizen.this, ReportsListActivity.class);
                    break;
                case 2: // Đánh giá dịch vụ
                    intent = new Intent(HomeActivityCitizen.this, FeedbackActivity.class);
                    break;
                case 3: // Thông tin tài khoản
                    intent = new Intent(HomeActivityCitizen.this, ProfileActivity.class);
                    break;
                case 4: // Đăng xuất
                    confirmLogout();
                    break;
            }

            if (intent != null) {
                startActivity(intent);
            }
        });

        builder.show();
    }

    private void showAccountMenu() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Tài khoản");
        String[] options = {"Đánh giá dịch vụ", "Đăng xuất"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                startActivity(new Intent(HomeActivityCitizen.this, FeedbackActivity.class));
            } else if (which == 1) {
                confirmLogout();
            }
        });
        builder.show();
    }

    private void confirmLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc chắn muốn đăng xuất?")
            .setPositiveButton("Đăng xuất", (d, w) -> doLogout())
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void doLogout() {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
        } catch (Exception ignore) {}

        Intent intent = new Intent(HomeActivityCitizen.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (vmMapView != null) {
            vmMapView.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vmMapView != null) {
            vmMapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (vmMapView != null) {
            vmMapView.onPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (vmMapView != null) {
            vmMapView.onStop();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (vmMapView != null) {
            vmMapView.onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vmMapView != null) {
            vmMapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (vmMapView != null) {
            vmMapView.onLowMemory();
        }
    }
}


