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

import android.speech.tts.TextToSpeech;

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
//import com.google.android.gms.location.Priority;
//import com.google.common.reflect.TypeToken;
//import com.google.gson.Gson;
//import com.google.gson.JsonArray;
//import com.google.gson.JsonObject;
//import java.lang.reflect.Type;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.vietmap.vietmapsdk.Vietmap;
import vn.vietmap.vietmapsdk.annotations.Icon;
import vn.vietmap.vietmapsdk.annotations.IconFactory;
import vn.vietmap.vietmapsdk.annotations.MarkerOptions;
import vn.vietmap.vietmapsdk.annotations.Polyline;
import vn.vietmap.vietmapsdk.annotations.PolylineOptions;
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



public class NearbyBinsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String VIETMAP_API_KEY = "ecdbd35460b2d399e18592e6264186757aaaddd8755b774c";

    private MapView mapView;
    private VietMapGL vietmapGL;
    private FusedLocationProviderClient fusedLocationClient;
    private ApiService apiService;

    private CardView cardBinInfo;
    private TextView tvBinCode, tvBinAddress, tvFillLevel, tvNearestBin;
    private ProgressBar progressFill;
    private Button btnReport, btnNavigate;

    private Bin selectedBin;
    private LocationCallback locationCallback;

    // Route / polyline
    private Polyline currentRoute;
    private final List<LatLng> routePolylinePoints = new java.util.ArrayList<>();
    private final List<JSONObject> routeSteps = new java.util.ArrayList<>();
    private int currentStepIndex = 0;
    private com.google.android.gms.location.LocationCallback navRouteCallback;
    private boolean isNavigating = false;
    private static final float STEP_TRIGGER_METERS = 30f; // 30m trước ngã rẽ

    // Text-to-Speech
    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;

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
        tvNearestBin = findViewById(R.id.tvNearestBin);
        tvBinAddress = findViewById(R.id.tvBinAddress);
        tvFillLevel = findViewById(R.id.tvFillLevel);
        progressFill = findViewById(R.id.progressFill);
        btnReport = findViewById(R.id.btnReport);
        btnNavigate = findViewById(R.id.btnNavigate);

        // Khởi tạo TextToSpeech để đọc hướng đi
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int res = textToSpeech.setLanguage(Locale.forLanguageTag("vi-VN"));
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech.setLanguage(Locale.getDefault());
                }
                ttsReady = true;
            } else {
                Toast.makeText(this, "Không khởi tạo được TextToSpeech", Toast.LENGTH_SHORT).show();
            }
        });

        initIcons(); // ✅ Khởi tạo icon

        mapView.getMapAsync(map -> {
            vietmapGL = map;
            vietmapGL.setStyle(
                    new Style.Builder().fromUri("https://maps.vietmap.vn/api/maps/light/styles.json?apikey=" + VIETMAP_API_KEY),
                    style -> {
                        // ✅ Gọi enableLocationComponent SAU khi style load xong
                        enableLocationComponent(style);
                        requestLocationAndFetchBins();
                    }
            );

        });
    }
    private void enableLocationComponent(@NonNull Style style) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            // ✅ Nếu chưa có quyền -> yêu cầu người dùng cho phép
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // ✅ Bật location component của VietMap
        LocationComponent locationComponent = vietmapGL.getLocationComponent();

        LocationComponentOptions customOptions = LocationComponentOptions.builder(this)
                .trackingGesturesManagement(true)
                .accuracyColor(ContextCompat.getColor(this, R.color.teal_200))
                .build();

        LocationComponentActivationOptions options =
                LocationComponentActivationOptions.builder(this, style)
                        .useDefaultLocationEngine(true) // ✅ cho phép tự cập nhật vị trí
                        .locationComponentOptions(customOptions)
                        .build();

        locationComponent.activateLocationComponent(options);
        locationComponent.setLocationComponentEnabled(true); // ⚡ QUAN TRỌNG: bật hiển thị vị trí
        locationComponent.setCameraMode(CameraMode.TRACKING);
        locationComponent.setRenderMode(RenderMode.COMPASS);

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
        vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 13));
        fetchNearbyBins(lat, lng);
    }

    // ------------------------- API -------------------------

    private void fetchNearbyBins(double latitude, double longitude) {
        apiService.getNearbyBins(latitude, longitude).enqueue(new Callback<List<Bin>>() {
            @Override
            public void onResponse(Call<List<Bin>> call, Response<List<Bin>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Bin> bins = response.body();
                    if (bins != null && !bins.isEmpty()) {
                        vietmapGL.clear();
                        for (Bin bin : bins) {
                            Log.d("Bin", "🗑 " + bin.getBinCode());
                            Icon icon = getSafeBinIcon(bin);
                            vietmapGL.addMarker(new MarkerOptions()
                                    .position(new LatLng(bin.getLatitude(), bin.getLongitude()))
                                    .title(bin.getBinCode())
                                    .snippet("Độ đầy: " + bin.getCurrentFill() + "%")
                                    .icon(icon));
                        }
                        Bin firstBin = bins.get(0);
                        if (tvNearestBin != null) {
                            tvNearestBin.setText("Thùng rác gần bạn nhất: " + firstBin.getBinCode());
                        }
                        vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(firstBin.getLatitude(), firstBin.getLongitude()), 14));
                        updateBinInfoCard(firstBin);
                    } else {
                        Toast.makeText(NearbyBinsActivity.this, "Không tìm thấy thùng rác gần đây.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(NearbyBinsActivity.this, "Không nhận được phản hồi hợp lệ từ server.", Toast.LENGTH_SHORT).show();
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
        if (bin == null) return;

        selectedBin = bin;

        tvBinCode.setText(bin.getBinCode());
        tvBinAddress.setText((bin.getStreet() != null ? bin.getStreet() : "Đường chưa xác định")
                + ", " + bin.getWardName() + ", " + bin.getProvinceName());
        int percent = (int) bin.getCurrentFill();
        tvFillLevel.setText(percent + "%");
        progressFill.setProgress(percent);
        // đổi màu progress theo phần trăm: <40 xanh, 40–79 vàng, >=80 đỏ
        if (percent >= 80) {
            progressFill.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.progress_bin_red));
        } else if (percent >= 40) {
            progressFill.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.progress_bin_yellow));
        } else {
            progressFill.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.progress_bin_green));
        }
        cardBinInfo.setVisibility(View.VISIBLE);

        btnReport.setOnClickListener(v -> {
            Intent intent = new Intent(NearbyBinsActivity.this, ReportBinActivity.class);
            intent.putExtra("bin_id", bin.getBinId()); // hoặc selectedBin.getBinId()
            intent.putExtra("bin_code", bin.getBinCode());
            intent.putExtra("bin_address", tvBinAddress.getText().toString());
            startActivity(intent);
        });

        // 🎯 Nút chỉ đường đến thùng rác này (gần nhất nếu là firstBin)
        if (btnNavigate != null) {
            btnNavigate.setOnClickListener(v -> navigateToBin(bin));
        }
    }

    // ------------------------- NAVIGATION -------------------------

    private void navigateToBin(Bin bin) {
        if (bin == null || vietmapGL == null) return;

        LocationComponent lc = vietmapGL.getLocationComponent();
        Location myLocation = (lc != null) ? lc.getLastKnownLocation() : null;
        if (myLocation == null) {
            Toast.makeText(this, "Không lấy được vị trí hiện tại", Toast.LENGTH_SHORT).show();
            return;
        }

        double fromLat = myLocation.getLatitude();
        double fromLng = myLocation.getLongitude();
        double toLat = bin.getLatitude();
        double toLng = bin.getLongitude();

        StringBuilder url = new StringBuilder("https://maps.vietmap.vn/api/route")
                .append("?api-version=1.1")
                .append("&apikey=").append(VIETMAP_API_KEY)
                .append("&vehicle=car")
                .append("&points_encoded=false")
                .append("&instructions=true");

        // Lưu ý: VietMap route thường dùng dạng point=lat,lng
        url.append("&point=").append(fromLat).append(",").append(fromLng);
        url.append("&point=").append(toLat).append(",").append(toLng);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url.toString()).build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(NearbyBinsActivity.this, "Lỗi route Vietmap: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() ->
                            Toast.makeText(NearbyBinsActivity.this, "Route API trả lỗi: " + response.code(), Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                try {
                    JSONObject json = new JSONObject(response.body().string());
                    JSONArray paths = json.optJSONArray("paths");
                    if (paths == null || paths.length() == 0) {
                        runOnUiThread(() ->
                                Toast.makeText(NearbyBinsActivity.this, "Không tìm thấy tuyến phù hợp", Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }

                    JSONObject path = paths.getJSONObject(0);
                    JSONArray coords = path.getJSONObject("points").getJSONArray("coordinates");

                    // Lưu polyline global để dùng cho điều hướng theo bước
                    routePolylinePoints.clear();
                    for (int i = 0; i < coords.length(); i++) {
                        JSONArray c = coords.getJSONArray(i); // [lng, lat]
                        routePolylinePoints.add(new LatLng(c.getDouble(1), c.getDouble(0)));
                    }

                    // Lưu toàn bộ step (gồm interval) để xác định điểm ngã rẽ
                    routeSteps.clear();
                    JSONArray instructionsArray = path.optJSONArray("instructions");
                    if (instructionsArray != null) {
                        for (int i = 0; i < instructionsArray.length(); i++) {
                            routeSteps.add(instructionsArray.getJSONObject(i));
                        }
                    }
                    currentStepIndex = 0;

                    runOnUiThread(() -> {
                        if (currentRoute != null) {
                            vietmapGL.removeAnnotation(currentRoute);
                        }
                        currentRoute = vietmapGL.addPolyline(
                                new PolylineOptions()
                                        .addAll(routePolylinePoints)
                                        .color(0xFF1976D2) // xanh dương
                                        .width(5f)
                        );

                        if (!routePolylinePoints.isEmpty()) {
                            vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(routePolylinePoints.get(0), 15f));
                        }

                        Toast.makeText(NearbyBinsActivity.this, "Đã vẽ tuyến đến thùng rác", Toast.LENGTH_SHORT).show();

                        // Bắt đầu điều hướng bằng giọng nói theo từng bước
                        startRouteVoiceNavigation();
                    });

                } catch (JSONException e) {
                    runOnUiThread(() ->
                            Toast.makeText(NearbyBinsActivity.this, "Lỗi parse JSON route: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    @SuppressWarnings({"MissingPermission"})
    private void startRouteVoiceNavigation() {
        if (!ttsReady || textToSpeech == null) {
            Toast.makeText(this, "TTS chưa sẵn sàng để đọc hướng đi", Toast.LENGTH_SHORT).show();
            return;
        }
        if (routePolylinePoints.isEmpty() || routeSteps.isEmpty()) {
            Toast.makeText(this, "Chưa có tuyến để điều hướng", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isNavigating) {
            Toast.makeText(this, "Đang điều hướng...", Toast.LENGTH_SHORT).show();
            return;
        }

        com.google.android.gms.location.LocationRequest request =
                com.google.android.gms.location.LocationRequest.create()
                        .setInterval(2000)
                        .setFastestInterval(1000)
                        .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY);

        navRouteCallback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult locationResult) {
                if (locationResult == null || routeSteps.isEmpty()) return;

                Location loc = locationResult.getLastLocation();
                if (loc == null) return;

                double lat = loc.getLatitude();
                double lng = loc.getLongitude();

                try {
                    if (currentStepIndex < routeSteps.size()) {
                        JSONObject step = routeSteps.get(currentStepIndex);
                        JSONArray interval = step.getJSONArray("interval"); // [startIdx, endIdx]
                        int endIdx = interval.getInt(1);
                        if (endIdx < 0 || endIdx >= routePolylinePoints.size()) {
                            currentStepIndex++;
                            return;
                        }
                        LatLng target = routePolylinePoints.get(endIdx);

                        float[] d = new float[1];
                        Location.distanceBetween(lat, lng, target.getLatitude(), target.getLongitude(), d);

                        String text = step.optString("text",
                                step.optString("instruction", "Tiếp tục theo tuyến đường"));

                        if (currentStepIndex == 0) {
                            // Bước đầu tiên: đọc ngay lập tức
                            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "STEP_0");
                            currentStepIndex++;
                        } else if (d[0] <= STEP_TRIGGER_METERS) {
                            // Khi còn cách ngã rẽ ~30m thì đọc bước tiếp theo
                            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, "STEP_" + currentStepIndex);
                            currentStepIndex++;
                        }

                        if (currentStepIndex >= routeSteps.size()) {
                            textToSpeech.speak("Bạn đã đến thùng rác.", TextToSpeech.QUEUE_ADD, null, "DONE");
                            stopRouteNavigationUpdates();
                        }
                    }
                } catch (Exception e) {
                    Log.e("NearbyBins", "NAV error: " + e.getMessage());
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(request, navRouteCallback, getMainLooper());
        isNavigating = true;
        Toast.makeText(this, "Bắt đầu điều hướng đến thùng rác", Toast.LENGTH_SHORT).show();
    }

    private void stopRouteNavigationUpdates() {
        if (fusedLocationClient != null && navRouteCallback != null) {
            fusedLocationClient.removeLocationUpdates(navRouteCallback);
        }
        isNavigating = false;
    }

    // ------------------------- ICONS -------------------------

    private void initIcons() {
        if (iconRed == null) iconRed = getBitmapFromVectorDrawable(R.drawable.ic_bin_red);
        if (iconYellow == null) iconYellow = getBitmapFromVectorDrawable(R.drawable.ic_bin_yellow);
        if (iconGreen == null) iconGreen = getBitmapFromVectorDrawable(R.drawable.ic_bin_green);
        if (iconDefault == null) iconDefault = getBitmapFromVectorDrawable(R.drawable.ic_bin_green);
    }

    private Icon getSafeBinIcon(Bin bin) {
        int percent = (int) bin.getCurrentFill();
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        stopRouteNavigationUpdates();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
