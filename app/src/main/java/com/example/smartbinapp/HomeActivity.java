package com.example.smartbinapp;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ImageView ivLocationPin, ivMenu;
    private LinearLayout btnHome, btnReport, btnAccount;
    private FloatingActionButton fabReport;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Khởi tạo Google Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Các phần còn lại giữ nguyên
        initializeViews();
        startEntranceAnimations();
        setupClickListeners();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 18)); // Zoom vào vị trí hiện tại
                        } else {
                            // Nếu không lấy được vị trí, zoom vào Đà Nẵng mặc định
                            LatLng danang = new LatLng(16.0678, 108.2208);
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(danang, 14));
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Thêm các marker thùng rác như cũ
        LatLng bin1 = new LatLng(16.061146, 108.224408);
        LatLng bin2 = new LatLng(16.0605, 108.2128);
        LatLng bin3 = new LatLng(16.0721, 108.2336);

        mMap.addMarker(new MarkerOptions()
                .position(bin1)
                .title("Thùng rác 1 - 75% đầy")
                .icon(getBitmapDescriptor(R.drawable.smart_bin_icon)));

        mMap.addMarker(new MarkerOptions()
                .position(bin2)
                .title("Thùng rác 2 - 45% đầy")
                .icon(getBitmapDescriptor(R.drawable.smart_bin_icon)));

        mMap.addMarker(new MarkerOptions()
                .position(bin3)
                .title("Thùng rác 3 - 90% đầy")
                .icon(getBitmapDescriptor(R.drawable.smart_bin_icon)));

        mMap.setOnMarkerClickListener(marker -> {
            Toast.makeText(this, marker.getTitle(), Toast.LENGTH_SHORT).show();
            return false;
        });
    }
    private BitmapDescriptor getBitmapDescriptor(int id) {
        Drawable vectorDrawable = ContextCompat.getDrawable(this, id);
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
    private void initializeViews() {
        ivMenu = findViewById(R.id.iv_menu);
        btnHome = findViewById(R.id.btn_home);
        btnReport = findViewById(R.id.btn_report);
        btnAccount = findViewById(R.id.btn_account);
        fabReport = findViewById(R.id.fab_report);
    }

    private void startEntranceAnimations() {
        // Animate top bar
        ObjectAnimator topBarAnimator = ObjectAnimator.ofFloat(findViewById(R.id.top_bar), "translationY", -100f, 0f);
        topBarAnimator.setDuration(800);
        topBarAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        topBarAnimator.start();

        // Animate bottom navigation
        ObjectAnimator bottomNavAnimator = ObjectAnimator.ofFloat(findViewById(R.id.bottom_navigation), "translationY", 100f, 0f);
        bottomNavAnimator.setDuration(800);
        bottomNavAnimator.setStartDelay(200);
        bottomNavAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        bottomNavAnimator.start();

        // Animate FAB
        ObjectAnimator fabAnimator = ObjectAnimator.ofFloat(fabReport, "scaleX", 0f, 1f);
        fabAnimator.setDuration(600);
        fabAnimator.setStartDelay(1000);
        fabAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        fabAnimator.start();

        ObjectAnimator fabAnimatorY = ObjectAnimator.ofFloat(fabReport, "scaleY", 0f, 1f);
        fabAnimatorY.setDuration(600);
        fabAnimatorY.setStartDelay(1000);
        fabAnimatorY.setInterpolator(new AccelerateDecelerateInterpolator());
        fabAnimatorY.start();
    }

    private void setupClickListeners() {
        // Menu button
        ivMenu.setOnClickListener(v -> {
            animateButtonClick(v);
            Toast.makeText(this, "Menu", Toast.LENGTH_SHORT).show();
        });

        // Navigation buttons
        btnHome.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, true);
            setActiveTab(btnReport, false);
            setActiveTab(btnAccount, false);
        });

        btnReport.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, false);
            setActiveTab(btnReport, true);
            setActiveTab(btnAccount, false);
            Toast.makeText(this, "Báo cáo", Toast.LENGTH_SHORT).show();
        });

        btnAccount.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, false);
            setActiveTab(btnReport, false);
            setActiveTab(btnAccount, true);
            Toast.makeText(this, "Tài khoản", Toast.LENGTH_SHORT).show();
        });

        // FAB Report
        fabReport.setOnClickListener(v -> {
            animateButtonClick(v);
            Toast.makeText(this, "Báo cáo vấn đề", Toast.LENGTH_SHORT).show();
        });
    }

    private void setActiveTab(LinearLayout tab, boolean isActive) {
        ImageView icon = (ImageView) tab.getChildAt(0);
        TextView text = (TextView) tab.getChildAt(1);
        
        if (isActive) {
            icon.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
            text.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            icon.setColorFilter(getResources().getColor(android.R.color.darker_gray));
            text.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    private void animateButtonClick(View view) {
        // Scale down animation
        ObjectAnimator scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f);
        scaleDown.setDuration(100);
        scaleDown.setInterpolator(new AccelerateDecelerateInterpolator());
        
        // Scale up animation
        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f);
        scaleUp.setDuration(100);
        scaleUp.setStartDelay(100);
        scaleUp.setInterpolator(new AccelerateDecelerateInterpolator());
        
        scaleDown.start();
        scaleUp.start();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    onMapReady(mMap); // G ọi lại để cập nhật vị trí
                }
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền vị trí để hiển thị vị trí hiện tại!", Toast.LENGTH_SHORT).show();
            }
        }
    }
} 