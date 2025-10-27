package com.example.smartbinapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.smartbinapp.model.Bin;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.vietmap.vietmapsdk.Vietmap;
import vn.vietmap.vietmapsdk.annotations.Icon;
import vn.vietmap.vietmapsdk.annotations.IconFactory;
import vn.vietmap.vietmapsdk.annotations.MarkerOptions;
import vn.vietmap.vietmapsdk.camera.CameraUpdateFactory;
import vn.vietmap.vietmapsdk.geometry.LatLng;
import vn.vietmap.vietmapsdk.maps.MapView;
import vn.vietmap.vietmapsdk.maps.Style;
import vn.vietmap.vietmapsdk.maps.VietMapGL;

public class NearbyBinsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String VIETMAP_API_KEY = "ecdbd35460b2d399e18592e6264186757aaaddd8755b774c";

    private MapView mapView;
    private VietMapGL vietmapGL;
    private FusedLocationProviderClient fusedLocationClient;
    private ApiService apiService;

    private CardView cardBinInfo;
    private TextView tvBinCode, tvBinAddress, tvFillLevel;
    private ProgressBar progressFill;
    private Button btnReport;

    private Bin selectedBin;
    private LocationCallback locationCallback;

    // 🗑 Icon cache
    private Bitmap iconRed, iconYellow, iconGreen, iconDefault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Vietmap.getInstance(this);
        setContentView(R.layout.activity_nearby_bins);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        cardBinInfo = findViewById(R.id.cardBinInfo);
        tvBinCode = findViewById(R.id.tvBinCode);
        tvBinAddress = findViewById(R.id.tvBinAddress);
        tvFillLevel = findViewById(R.id.tvFillLevel);
        progressFill = findViewById(R.id.progressFill);
        btnReport = findViewById(R.id.btnReport);

        initIcons(); // ✅ Khởi tạo icon giống HomeActivity

        mapView.getMapAsync(map -> {
            vietmapGL = map;
            vietmapGL.setStyle(
                    new Style.Builder().fromUri("https://maps.vietmap.vn/api/maps/light/styles.json?apikey=" + VIETMAP_API_KEY),
                    style -> requestLocationAndFetchBins()
            );
        });
    }

    // ------------------------- LOCATION -------------------------

    private void requestLocationAndFetchBins() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        updateMapAndFetch(location);
                    } else {
                        requestNewLocationUpdate();
                    }
                })
                .addOnFailureListener(e -> requestNewLocationUpdate());
    }

    private void requestNewLocationUpdate() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(1000)
                .setWaitForAccurateLocation(true)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                fusedLocationClient.removeLocationUpdates(this);
                if (locationResult.getLastLocation() != null) {
                    updateMapAndFetch(locationResult.getLastLocation());
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        }
    }

    private void updateMapAndFetch(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 15));
        fetchNearbyBins(lat, lng);
    }

    // ------------------------- API -------------------------

    private void fetchNearbyBins(double latitude, double longitude) {
        apiService.getNearbyBins(latitude, longitude).enqueue(new Callback<List<Bin>>() {
            @Override
            public void onResponse(Call<List<Bin>> call, Response<List<Bin>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Bin nearestBin = response.body().get(0);
                    selectedBin = nearestBin;

                    vietmapGL.clear();
                    Icon icon = getSafeBinIcon(nearestBin);
                    vietmapGL.addMarker(new MarkerOptions()
                            .position(new LatLng(nearestBin.getLatitude(), nearestBin.getLongitude()))
                            .title(nearestBin.getBinCode())
                            .snippet("Độ đầy: " + (int)((nearestBin.getCurrentFill() / nearestBin.getCapacity()) * 100) + "%")
                            .icon(icon)
                    );

                    vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(nearestBin.getLatitude(), nearestBin.getLongitude()), 16));

                    updateBinInfoCard(nearestBin);
                } else {
                    Toast.makeText(NearbyBinsActivity.this, "Không tìm thấy thùng rác gần đây.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Bin>> call, Throwable t) {
                Toast.makeText(NearbyBinsActivity.this, "Lỗi tải dữ liệu: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ------------------------- UI -------------------------

    private void updateBinInfoCard(Bin bin) {
        tvBinCode.setText(bin.getBinCode());
        tvBinAddress.setText(bin.getStreet() + ", " + bin.getWardName() + ", " + bin.getProvinceName());
        int percent = (int) ((bin.getCurrentFill() / bin.getCapacity()) * 100);
        tvFillLevel.setText(percent + "%");
        progressFill.setProgress(percent);
        cardBinInfo.setVisibility(View.VISIBLE);

        btnReport.setOnClickListener(v -> {
            if (selectedBin != null) {
                Intent intent = new Intent(NearbyBinsActivity.this, ReportBinActivity.class);
                intent.putExtra("bin_id", selectedBin.getBinId());
                intent.putExtra("bin_code", selectedBin.getBinCode());
                intent.putExtra("bin_address", tvBinAddress.getText().toString());
                startActivity(intent);
            }
        });
    }

    // ------------------------- ICONS -------------------------

    private void initIcons() {
        if (iconRed == null) iconRed = getBitmapFromVectorDrawable(R.drawable.ic_bin_red);
        if (iconYellow == null) iconYellow = getBitmapFromVectorDrawable(R.drawable.ic_bin_yellow);
        if (iconGreen == null) iconGreen = getBitmapFromVectorDrawable(R.drawable.ic_bin_green);
        if (iconDefault == null) iconDefault = getBitmapFromVectorDrawable(R.drawable.ic_bin_green);
    }

    private Icon getSafeBinIcon(Bin bin) {
        int percent = (int) ((bin.getCurrentFill() / bin.getCapacity()) * 100);
        Bitmap targetBitmap;
        if (percent >= 80) targetBitmap = iconRed;
        else if (percent >= 40) targetBitmap = iconYellow;
        else targetBitmap = iconGreen;

        if (targetBitmap == null) targetBitmap = iconDefault;
        return IconFactory.getInstance(this).fromBitmap(targetBitmap);
    }

    private Bitmap getBitmapFromVectorDrawable(int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableId);
        if (drawable == null) return null;

        drawable = drawable.mutate();
        int size = (int) (30 * getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);
        return bitmap;
    }

    // ---------------- Permissions & Lifecycle ----------------
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationAndFetchBins();
            } else {
                Toast.makeText(this, "Cần quyền truy cập vị trí để tìm thùng rác gần bạn.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}