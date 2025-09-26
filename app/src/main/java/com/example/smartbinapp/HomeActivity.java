package com.example.smartbinapp;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import vn.vietmap.vietmapsdk.Vietmap;
import vn.vietmap.vietmapsdk.WellKnownTileServer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.smartbinapp.model.Bin;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.messaging.FirebaseMessaging;

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
import vn.vietmap.vietmapsdk.location.LocationComponentOptions;
import vn.vietmap.vietmapsdk.location.engine.LocationEngine;
import vn.vietmap.vietmapsdk.location.engine.LocationEngineCallback;
import vn.vietmap.vietmapsdk.location.engine.LocationEngineRequest;
import vn.vietmap.vietmapsdk.location.engine.LocationEngineResult;
import vn.vietmap.vietmapsdk.location.modes.CameraMode;
import vn.vietmap.vietmapsdk.location.modes.RenderMode;
import vn.vietmap.vietmapsdk.maps.MapView;
import vn.vietmap.vietmapsdk.maps.OnMapReadyCallback;
import vn.vietmap.vietmapsdk.maps.Style;
import vn.vietmap.vietmapsdk.maps.VietMapGL;


public class HomeActivity extends AppCompatActivity {

    private ImageView ivMenu;
    private LinearLayout btnHome, btnReport, btnShowTask, btnAccount;
    private FloatingActionButton fabReport;

    private MapView mapView;
    private VietMapGL vietmapGL;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private FusedLocationProviderClient fusedLocationClient;
    private FloatingActionButton fabMyLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Khởi tạo Vietmap SDK trước khi inflate layout
        Vietmap.getInstance(this);


        setContentView(R.layout.activity_home);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }



        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Lấy token thất bại", task.getException());
                        return;
                    }
                    // Token
                    String token = task.getResult();
                    Log.d("FCM", "FCM Token (HomeActivity): " + token);

                    // Có thể show luôn ra màn hình
                    Toast.makeText(HomeActivity.this, "FCM Token: " + token, Toast.LENGTH_LONG).show();
                });
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapView = findViewById(R.id.vmMapView);
        mapView.onCreate(savedInstanceState);

        initializeViews();
        startEntranceAnimations();
        setupClickListeners();

        mapView.getMapAsync(map -> {
            vietmapGL = map;
            vietmapGL.setStyle(
                    new Style.Builder().fromUri("https://maps.vietmap.vn/api/maps/light/styles.json?apikey=ecdbd35460b2d399e18592e6264186757aaaddd8755b774c"),
                    style -> {
                        if (ActivityCompat.checkSelfPermission(this,
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            enableLocationComponent();
                        } else {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    LOCATION_PERMISSION_REQUEST_CODE);
                        }
                        loadBinsFromApi();
                    }
            );
        });
    }


    private void loadBinsFromApi() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        apiService.getAllBinDTOs().enqueue(new Callback<List<Bin>>() {
            @Override
            public void onResponse(Call<List<Bin>> call, Response<List<Bin>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Bin bin : response.body()) {
                        int percent = (int) ((bin.getCurrentFill() / bin.getCapacity()) * 100);

                        LatLng position = new LatLng(bin.getLatitude(), bin.getLongitude());
                        String title = bin.getBinCode() + " - " + percent + "% đầy";
                        String snippet = bin.getStreet() + ", " + bin.getWardName() + ", " + bin.getProvinceName();

                        int iconRes;
                        if (percent > 80) {
                            iconRes = R.drawable.ic_bin_red;
                        } else if (percent > 40) {
                            iconRes = R.drawable.ic_bin_yellow;
                        } else {
                            iconRes = R.drawable.ic_bin_green;
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
        locationComponent.setLocationComponentEnabled(true);
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
            fakeLocation.setLatitude(15.969114);
            fakeLocation.setLongitude(108.260765);

            // Gán cho LocationComponent
            locationComponent.forceLocationUpdate(fakeLocation);

            // Camera cũng bay đến đó
            vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(15.969114, 108.260765), 16));
        }
    }




    private void initializeViews() {
        ivMenu = findViewById(R.id.iv_menu);
        btnHome = findViewById(R.id.btn_home);
        btnShowTask = findViewById(R.id.btn_showtask);
        btnAccount = findViewById(R.id.btn_account);
        fabReport = findViewById(R.id.fab_report);
        btnReport = findViewById(R.id.btn_report);
        fabMyLocation = findViewById(R.id.fab_my_location);
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
            setActiveTab(btnShowTask, false);
            setActiveTab(btnAccount, false);
        });

        btnReport.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, false);
            setActiveTab(btnReport, true);
            setActiveTab(btnShowTask, false);
            setActiveTab(btnAccount, false);
            Intent intent = new Intent(HomeActivity.this, TaskSummaryActivity.class);
            startActivity(intent);
            finish();
        });

        btnShowTask.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, false);
            setActiveTab(btnShowTask, true);
            setActiveTab(btnReport, false);
            setActiveTab(btnAccount, false);
            Intent intent = new Intent(HomeActivity.this, TaskSummaryActivity.class);
            startActivity(intent);
            finish();
        });

        btnAccount.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, false);
            setActiveTab(btnReport, false);
            setActiveTab(btnShowTask, false);
            setActiveTab(btnAccount, true);
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
            finish();
        });

        fabReport.setOnClickListener(v -> {
            animateButtonClick(v);
            Toast.makeText(this, "Báo cáo vấn đề", Toast.LENGTH_SHORT).show();
        });


        fabMyLocation.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED && vietmapGL != null) {

                LocationComponent locationComponent = vietmapGL.getLocationComponent();
                if (locationComponent.getLastKnownLocation() != null) {
                    LatLng myLocation = new LatLng(
                            locationComponent.getLastKnownLocation().getLatitude(),
                            locationComponent.getLastKnownLocation().getLongitude()
                    );
                    vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 16));
                } else {
                    Toast.makeText(this, "Không lấy được vị trí hiện tại", Toast.LENGTH_SHORT).show();
                }
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationComponent();
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền vị trí để xem vị trí hiện tại", Toast.LENGTH_SHORT).show();
            }
        }
    }



    // Lifecycle MapView
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
