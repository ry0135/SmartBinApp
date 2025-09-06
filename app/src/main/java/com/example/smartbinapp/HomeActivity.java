package com.example.smartbinapp;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.smartbinapp.model.Bin;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.vietmap.vietmapsdk.Vietmap;
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

public class HomeActivity extends AppCompatActivity {

    private ImageView ivMenu;
    private LinearLayout btnHome, btnReport, btnAccount;
    private FloatingActionButton fabReport;

    private MapView mapView;
    private VietMapGL vietmapGL;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private FusedLocationProviderClient fusedLocationClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Khởi tạo SDK VietMap
        Vietmap.getInstance(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mapView = findViewById(R.id.vmMapView);
        mapView.onCreate(savedInstanceState);

        initializeViews();
        startEntranceAnimations();
        setupClickListeners();

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(VietMapGL map) {
                vietmapGL = map;

                vietmapGL.setStyle(new Style.Builder()
                                .fromUri("https://maps.vietmap.vn/api/maps/light/styles.json?apikey=ecdbd35460b2d399e18592e6264186757aaaddd8755b774c"),
                        style -> {
                            // Sau khi load style thì check quyền vị trí
                            if (ActivityCompat.checkSelfPermission(HomeActivity.this,
                                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                                enableLocationComponent();

                                fusedLocationClient.getLastLocation()
                                        .addOnSuccessListener(HomeActivity.this, location -> {
                                            if (location != null) {
                                                LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                                vietmapGL.animateCamera(CameraUpdateFactory
                                                        .newLatLngZoom(myLatLng, 15));
                                            } else {
                                                // Nếu không lấy được thì zoom về Đà Nẵng mặc định
                                                vietmapGL.animateCamera(CameraUpdateFactory
                                                        .newLatLngZoom(new LatLng(16.0678, 108.2208), 12));
                                            }
                                        });
                            } else {
                                // Yêu cầu quyền vị trí nếu chưa có
                                ActivityCompat.requestPermissions(HomeActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        LOCATION_PERMISSION_REQUEST_CODE);
                            }

                            // Luôn load thùng rác sau khi map và style xong
                            loadBinsFromApi();
                        });
            }
        });
    }

    private void loadBinsFromApi() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        apiService.getAllBins().enqueue(new Callback<List<Bin>>() {
            @Override
            public void onResponse(Call<List<Bin>> call, Response<List<Bin>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Bin bin : response.body()) {
                        int percent = (int) ((bin.getCurrentFill() / bin.getCapacity()) * 100);

                        LatLng position = new LatLng(bin.getLatitude(), bin.getLongitude());
                        String title = bin.getBinCode() + " - " + percent + "% đầy";
                        String snippet = bin.getStreet() + ", " + bin.getWard() + ", " + bin.getCity();

                        // Chọn icon theo mức đầy
                        int iconRes;
                        if (percent > 80) {
                            iconRes = R.drawable.ic_bin_red; // thùng đầy
                        } else if (percent > 40) {
                            iconRes = R.drawable.ic_bin_yellow; // thùng trung bình
                        } else {
                            iconRes = R.drawable.ic_bin_green; // thùng trống
                        }

                        vietmapGL.addMarker(new MarkerOptions()
                                .position(position)
                                .title(title)
                                .snippet(snippet)
                                .icon(IconFactory.getInstance(HomeActivity.this)
                                        .fromBitmap(getBitmapFromVectorDrawable(iconRes))));
                    }

                    vietmapGL.setOnMarkerClickListener(marker -> {
                        Toast.makeText(HomeActivity.this,
                                marker.getTitle() + "\n" + marker.getSnippet(),
                                Toast.LENGTH_LONG).show();
                        return false;
                    });

                    // Zoom vào thùng đầu tiên
                    if (!response.body().isEmpty()) {
                        Bin firstBin = response.body().get(0);
                        vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(firstBin.getLatitude(), firstBin.getLongitude()), 14));
                    }
                } else {
                    Toast.makeText(HomeActivity.this, "Không tải được danh sách thùng rác", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Bin>> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Lấy LocationComponent
        LocationComponent locationComponent = vietmapGL.getLocationComponent();

        // Tạo options để kích hoạt
        LocationComponentActivationOptions options =
                LocationComponentActivationOptions.builder(this, vietmapGL.getStyle())
                        .useDefaultLocationEngine(true)
                        .build();

        // Kích hoạt
        locationComponent.activateLocationComponent(options);

        // Hiển thị vị trí hiện tại
        locationComponent.setLocationComponentEnabled(true);

        // Camera tracking user location
        locationComponent.setCameraMode(CameraMode.TRACKING);
        locationComponent.setRenderMode(RenderMode.COMPASS);
    }

    private void initializeViews() {
        ivMenu = findViewById(R.id.iv_menu);
        btnHome = findViewById(R.id.btn_home);
        btnReport = findViewById(R.id.btn_report);
        btnAccount = findViewById(R.id.btn_account);
        fabReport = findViewById(R.id.fab_report);
    }

    private void startEntranceAnimations() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(findViewById(R.id.top_bar), "translationY", -100f, 0f);
        animator.setDuration(800);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();


        ObjectAnimator bottomNavAnimator = ObjectAnimator.ofFloat(findViewById(R.id.bottom_navigation),
                "translationY", 100f, 0f);
        bottomNavAnimator.setDuration(800);
        bottomNavAnimator.setStartDelay(200);
        bottomNavAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        bottomNavAnimator.start();

        ObjectAnimator fabScaleX = ObjectAnimator.ofFloat(fabReport, "scaleX", 0f, 1f);
        ObjectAnimator fabScaleY = ObjectAnimator.ofFloat(fabReport, "scaleY", 0f, 1f);
        fabScaleX.setDuration(600);
        fabScaleY.setDuration(600);
        fabScaleX.setStartDelay(1000);
        fabScaleY.setStartDelay(1000);
        fabScaleX.start();
        fabScaleY.start();
    }

    private void setupClickListeners() {
        ivMenu.setOnClickListener(v -> {
            animateButtonClick(v);
            Toast.makeText(this, "Menu", Toast.LENGTH_SHORT).show();
        });

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

        fabReport.setOnClickListener(v -> {
            animateButtonClick(v);
            Toast.makeText(this, "Báo cáo vấn đề", Toast.LENGTH_SHORT).show();
        });
    }


    private void setActiveTab(LinearLayout tab, boolean isActive) {
        ImageView icon = (ImageView) tab.getChildAt(0);
        TextView text = (TextView) tab.getChildAt(1);

        int activeColor = getResources().getColor(android.R.color.holo_green_dark);
        int inactiveColor = getResources().getColor(android.R.color.darker_gray);

        icon.setColorFilter(isActive ? activeColor : inactiveColor);
        text.setTextColor(isActive ? activeColor : inactiveColor);
    }

    private void animateButtonClick(View view) {
        ObjectAnimator scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f);
        scaleDown.setDuration(100);
        scaleDown.start();

        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f);
        scaleUp.setDuration(100);
        scaleUp.setStartDelay(100);
        scaleUp.start();
    }

    private Bitmap getBitmapFromVectorDrawable(int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableId);
        if (drawable == null) return null;

        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    // Lifecycle MapView cần override
    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
