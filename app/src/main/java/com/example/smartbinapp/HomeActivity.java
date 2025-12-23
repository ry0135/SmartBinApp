package com.example.smartbinapp;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.smartbinapp.model.Bin;
import com.example.smartbinapp.model.Notification;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.example.smartbinapp.service.BinWebSocketService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.vietmap.vietmapsdk.Vietmap;
import vn.vietmap.vietmapsdk.annotations.Icon;
import vn.vietmap.vietmapsdk.annotations.IconFactory;
import vn.vietmap.vietmapsdk.annotations.Marker;
import vn.vietmap.vietmapsdk.annotations.MarkerOptions;
import vn.vietmap.vietmapsdk.camera.CameraUpdateFactory;
import vn.vietmap.vietmapsdk.geometry.LatLng;
import vn.vietmap.vietmapsdk.location.LocationComponent;
import vn.vietmap.vietmapsdk.location.LocationComponentActivationOptions;
import vn.vietmap.vietmapsdk.location.LocationComponentOptions;
import vn.vietmap.vietmapsdk.location.modes.CameraMode;
import vn.vietmap.vietmapsdk.location.modes.RenderMode;
import vn.vietmap.vietmapsdk.maps.MapView;
import vn.vietmap.vietmapsdk.maps.Style;
import vn.vietmap.vietmapsdk.maps.VietMapGL;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // UI Components
    private ImageView ivnotification;
    private LinearLayout btnHome, btnReport, btnShowTask, btnAccount;
    private FloatingActionButton fabnearBin, fabMyLocation;
    private MapView mapView;
    private DrawerLayout drawerLayout;

    // Map & Location
    private VietMapGL vietmapGL;
    private FusedLocationProviderClient fusedLocationClient;

    // Cache icons and markers
    private Bitmap iconRed, iconYellow, iconGreen, iconGrey, iconDefault;
    private final Map<Integer, Marker> markerMap = new HashMap<>();

    // Realtime WebSocket
    private final BinWebSocketService wsService = new BinWebSocketService();

    private final Map<Marker, Bin> binDataMap = new HashMap<>();

    private TextView tvBadge;

    // 🔥 Flag để biết map đã được khởi tạo chưa
    private boolean isMapInitialized = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Vietmap.getInstance(this);
        setContentView(R.layout.activity_home);

        // Initialization
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapView = findViewById(R.id.vmMapView);
        mapView.onCreate(savedInstanceState);
        initializeFCMAndPermissions();
        initializeViews();
        startEntranceAnimations();
        setupClickListeners();

        // 🔥 Khởi tạo map
        initializeMap();
    }

    // 🔥 Tách riêng phần khởi tạo map
    private void initializeMap() {
        if (isMapInitialized) {
            Log.d(TAG, "Map đã được khởi tạo, bỏ qua");
            return;
        }

        mapView.getMapAsync(map -> {
            vietmapGL = map;
            vietmapGL.setStyle(
                    new Style.Builder().fromUri("https://maps.vietmap.vn/api/maps/light/styles.json?apikey=ecdbd35460b2d399e18592e6264186757aaaddd8755b774c"),
                    this::onStyleLoaded
            );
            isMapInitialized = true;
        });
    }

    private void onStyleLoaded(Style style) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableLocationComponent();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
        // Load danh sách thùng ban đầu
        mapView.postDelayed(this::loadBinsFromApi, 800);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (vietmapGL != null && vietmapGL.getStyle() != null) {
                    enableLocationComponent();
                }
            } else {
                Toast.makeText(this, "Cần quyền truy cập vị trí để sử dụng tính năng này", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadBinsFromApi() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.getAllBinDTOs().enqueue(new Callback<List<Bin>>() {
            @Override
            public void onResponse(Call<List<Bin>> call, Response<List<Bin>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    initIcons();
                    for (Bin bin : response.body()) {
                        addOrUpdateMarker(bin, false);
                    }

                    vietmapGL.setOnMarkerClickListener(marker -> {
                        Bin clickedBin = binDataMap.get(marker);
                        if (clickedBin != null) {
                            showBinActionBottomSheet(clickedBin, marker);
                        } else {
                            Log.w("MarkerClick", "⚠️ Không tìm thấy dữ liệu bin cho marker: " + marker.getTitle());
                        }
                        return true;
                    });
                } else {
                    Toast.makeText(HomeActivity.this, "Không tải được danh sách thùng rác", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Bin>> call, Throwable t) {
                Log.e(TAG, "Lỗi kết nối API: " + t.getMessage(), t);
                Toast.makeText(HomeActivity.this, "Lỗi kết nối mạng ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBinActionBottomSheet(Bin bin, Marker marker) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.layout_bin_action_bottomsheet);

        TextView tvBinTitle = dialog.findViewById(R.id.tvBinTitle);
        TextView tvBinInfo = dialog.findViewById(R.id.tvBinInfo);
        Button btnViewDetail = dialog.findViewById(R.id.btnViewDetail);
        Button btnReportBin = dialog.findViewById(R.id.btnReportBin);

        tvBinTitle.setText("🗑 Thùng " + marker.getTitle());
        tvBinInfo.setText(marker.getSnippet());

        btnViewDetail.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, BinDetailActivity.class);
            intent.putExtra("binId", bin.getBinId());
            intent.putExtra("binCode", bin.getBinCode());
            intent.putExtra("fill", bin.getCurrentFill());
            intent.putExtra("street", bin.getStreet());
            intent.putExtra("ward", bin.getWardName());
            intent.putExtra("province", bin.getProvinceName());
            intent.putExtra("lat", bin.getLatitude());
            intent.putExtra("lng", bin.getLongitude());
            intent.putExtra("status", bin.getStatus());
            startActivity(intent);
            dialog.dismiss();
        });

        btnReportBin.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ReportBinActivity.class);
            intent.putExtra("bin_id", bin.getBinId());
            intent.putExtra("bin_address",
                    (bin.getStreet() != null ? bin.getStreet() : "Đường chưa xác định") + ", " +
                            (bin.getWardName() != null ? bin.getWardName() : "Phường chưa rõ") + ", " +
                            (bin.getProvinceName() != null ? bin.getProvinceName() : "Tỉnh/TP chưa rõ"));
            intent.putExtra("bin_code", bin.getBinCode());
            Log.d("ReportIntent", "Street: " + bin.getStreet());
            Log.d("ReportIntent", "Ward: " + bin.getWardName());
            Log.d("ReportIntent", "Province: " + bin.getProvinceName());
            startActivity(intent);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void onBinUpdateReceived(Bin updatedBin) {
        runOnUiThread(() -> {
            Log.d(TAG, "🛰 Received update: " + updatedBin.getBinCode() + " | binId=" + updatedBin.getBinId());
            addOrUpdateMarker(updatedBin, true);
        });
    }

    private void addOrUpdateMarker(Bin bin, boolean isRealtimeUpdate) {
        if (vietmapGL == null) return;

        int percent = (int) bin.getCurrentFill();
        Icon icon = getSafeBinIcon(bin);
        String title = bin.getBinCode() + " - " + percent + "% đầy";
        String snippet = isRealtimeUpdate ?
                "Cập nhật lúc: " + System.currentTimeMillis() :
                bin.getWardName() + ", " + bin.getProvinceName();

        if (bin.getBinId() > 0 && markerMap.containsKey(bin.getBinId())) {
            Marker oldMarker = markerMap.remove(bin.getBinId());
            if (oldMarker != null) vietmapGL.removeMarker(oldMarker);
            Log.d(TAG, "Removed old marker for BinID: " + bin.getBinId());
        }

        Marker marker = vietmapGL.addMarker(new MarkerOptions()
                .position(new LatLng(bin.getLatitude(), bin.getLongitude()))
                .title(title)
                .snippet(snippet)
                .icon(icon)
        );

        if (bin.getBinId() > 0) {
            markerMap.put(bin.getBinId(), marker);
        } else {
            markerMap.put(marker.hashCode(), marker);
        }

        binDataMap.put(marker, bin);

        Log.d(TAG, "Added new marker for BinID: " + bin.getBinId() + " with fill: " + percent + "%");
    }

    private void initIcons() {
        if (iconRed == null) iconRed = getBitmapFromVectorDrawable(R.drawable.ic_bin_red);
        if (iconYellow == null) iconYellow = getBitmapFromVectorDrawable(R.drawable.ic_bin_yellow);
        if (iconGreen == null) iconGreen = getBitmapFromVectorDrawable(R.drawable.ic_bin_green);
        if (iconGrey == null) iconGrey = getBitmapFromVectorDrawable(R.drawable.ic_bin_grey);
        if (iconDefault == null) iconDefault = getBitmapFromVectorDrawable(R.drawable.ic_bin_green);
    }

    private Icon getSafeBinIcon(Bin bin) {
        Bitmap targetBitmap;

        if (bin.getStatus() == 2) {
            targetBitmap = iconGrey;
        } else {
            int percent = (int) bin.getCurrentFill();
            if (percent >= 80) targetBitmap = iconRed;
            else if (percent >= 40) targetBitmap = iconYellow;
            else targetBitmap = iconGreen;
        }

        if (targetBitmap == null) {
            targetBitmap = iconDefault;
        }

        return IconFactory.getInstance(this).fromBitmap(targetBitmap);
    }

    @Nullable
    private Bitmap getBitmapFromVectorDrawable(int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableId);
        if (drawable == null) {
            Log.e(TAG, "Lỗi: Không tìm thấy Drawable ID: " + drawableId);
            return null;
        }

        drawable = drawable.mutate();

        try {
            int targetWidthPx = dpToPx(30);
            int targetHeightPx = dpToPx(30);
            int densityDpi = getResources().getDisplayMetrics().densityDpi;

            Bitmap bitmap = Bitmap.createBitmap(
                    targetWidthPx,
                    targetHeightPx,
                    Bitmap.Config.ARGB_8888
            );

            bitmap.setDensity(densityDpi);

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, targetWidthPx, targetHeightPx);
            drawable.draw(canvas);

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Lỗi nghiêm trọng khi tạo Bitmap từ Vector Drawable.", e);
            return null;
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void initializeFCMAndPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Log.d("FCM", "Token: " + task.getResult());
            }
        });
    }

    @SuppressWarnings("MissingPermission")
    private void enableLocationComponent() {
        LocationComponent locationComponent = vietmapGL.getLocationComponent();
        LocationComponentOptions customOptions = LocationComponentOptions.builder(this)
                .foregroundDrawable(R.drawable.ic_my_location)
                .backgroundDrawable(R.drawable.ic_my_location)
                .build();

        LocationComponentActivationOptions options =
                LocationComponentActivationOptions.builder(this, vietmapGL.getStyle())
                        .useDefaultLocationEngine(true)
                        .locationComponentOptions(customOptions)
                        .build();

        locationComponent.activateLocationComponent(options);
        locationComponent.setLocationComponentEnabled(true);
        locationComponent.setCameraMode(CameraMode.TRACKING);
        locationComponent.setRenderMode(RenderMode.NORMAL);

        Location last = locationComponent.getLastKnownLocation();
        if (last != null) {
            vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(last.getLatitude(), last.getLongitude()), 16));
        } else {
            vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(15.969114, 108.260765), 16));
        }
    }

    private void moveToMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && vietmapGL != null) {
            LocationComponent lc = vietmapGL.getLocationComponent();
            if (lc.getLastKnownLocation() != null) {
                LatLng myLocation = new LatLng(
                        lc.getLastKnownLocation().getLatitude(),
                        lc.getLastKnownLocation().getLongitude()
                );
                vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 16));
            } else {
                Toast.makeText(this, "Không lấy được vị trí hiện tại", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchUnreadCount() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String savedUserId = prefs.getString("userId", "0");
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        apiService.getUnreadCount(savedUserId).enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(Call<Integer> call, Response<Integer> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateNotificationBadge(response.body());
                }
            }

            @Override
            public void onFailure(Call<Integer> call, Throwable t) {
                Log.e(TAG, "Error fetching unread count: " + t.getMessage());
            }
        });
    }

    private void updateNotificationBadge(int unreadCount) {
        if (unreadCount > 0) {
            tvBadge.setText(String.valueOf(unreadCount));
            tvBadge.setVisibility(View.VISIBLE);
        } else {
            tvBadge.setVisibility(View.GONE);
        }
        Log.d("BADGE", "Unread = " + unreadCount + ", tvBadge = " + tvBadge);
    }

    private void initializeViews() {
        ivnotification = findViewById(R.id.iv_notification);
        btnHome = findViewById(R.id.btn_home);
        btnShowTask = findViewById(R.id.btn_showtask);
        btnAccount = findViewById(R.id.btn_account);
        fabnearBin = findViewById(R.id.fab_nearbin);
        btnReport = findViewById(R.id.btn_report);
        fabMyLocation = findViewById(R.id.fab_my_location);
        tvBadge = findViewById(R.id.tv_notification_badge);

        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        int savedRole = prefs.getInt("role", 0);

        if (savedRole == 4) {
            btnShowTask.setVisibility(View.GONE);
            Log.d("RoleCheck", "Đã ẩn nút Nhiệm vụ vì người dùng là citizen");
        }
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
    }

    private void setupClickListeners() {
        ivnotification.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, NotificationListActivity.class));
        });

        // 🔥 Giữ HomeActivity trong stack để có thể quay lại
        btnHome.setOnClickListener(v -> {
            // Không làm gì vì đang ở Home rồi
            Toast.makeText(this, "Đang ở trang chủ", Toast.LENGTH_SHORT).show();
        });

        btnAccount.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        btnShowTask.setOnClickListener(v -> startActivity(new Intent(this, TaskSummaryActivity.class)));

        fabMyLocation.setOnClickListener(v -> moveToMyLocation());
        fabnearBin.setOnClickListener(v -> startActivity(new Intent(this, NearbyBinsActivity.class)));

        btnReport.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ReportsListActivity.class);
            startActivity(intent);
        });
    }

    // 🔥 QUAN TRỌNG: Thêm onStart() để reconnect WebSocket
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();

        // Reconnect WebSocket khi quay lại activity
        if (!wsService.isConnected()) {
            wsService.connect();
            wsService.setListener(this::onBinUpdateReceived);
            Log.d(TAG, "✅ WebSocket reconnected in onStart()");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        fetchUnreadCount();

        // 🔥 Reload bins khi quay lại
        if (vietmapGL != null && isMapInitialized) {
            Log.d(TAG, "🔄 Reloading bins on resume");
            loadBinsFromApi();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
        // ⚠️ KHÔNG disconnect WebSocket ở đây nữa để giữ kết nối
        // wsService.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        wsService.disconnect();
        Log.d(TAG, "🔴 WebSocket disconnected in onDestroy()");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}