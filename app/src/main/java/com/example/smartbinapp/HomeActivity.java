package com.example.smartbinapp;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
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

import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
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
    private LinearLayout btnHome, btnShowTask, btnAccount;
    private FloatingActionButton fabReport, fabMenu;

    private MapView mapView;
    private VietMapGL vietmapGL;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private FusedLocationProviderClient fusedLocationClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            // Khởi tạo SDK VietMap với error handling
        Vietmap.getInstance(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        } catch (Exception e) {
            // Log error nhưng không crash app
            android.util.Log.e("HomeActivity", "VietMap initialization failed: " + e.getMessage());
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        try {
        mapView = findViewById(R.id.vmMapView);
            if (mapView != null) {
        mapView.onCreate(savedInstanceState);
            }
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "MapView initialization failed: " + e.getMessage());
        }

        initializeViews();
        startEntranceAnimations();
        setupClickListeners();


        // Khôi phục map loading với error handling
        try {
            if (mapView != null) {
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(VietMapGL map) {
                        try {
                vietmapGL = map;

                vietmapGL.setStyle(new Style.Builder()
                                .fromUri("https://maps.vietmap.vn/api/maps/light/styles.json?apikey=ecdbd35460b2d399e18592e6264186757aaaddd8755b774c"),
                        style -> {
                                        try {
                                            // Delay để tránh performance issues
                                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                                try {
                            // Sau khi load style thì check quyền vị trí
                            if (ActivityCompat.checkSelfPermission(HomeActivity.this,
                                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                                enableLocationComponent();

                                                        if (fusedLocationClient != null) {
                                fusedLocationClient.getLastLocation()
                                        .addOnSuccessListener(HomeActivity.this, location -> {
                                                                        try {
                                                                            if (location != null && vietmapGL != null) {
                                                LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                                vietmapGL.animateCamera(CameraUpdateFactory
                                                        .newLatLngZoom(myLatLng, 15));
                                                                            } else if (vietmapGL != null) {
                                                // Nếu không lấy được thì zoom về Đà Nẵng mặc định
                                                vietmapGL.animateCamera(CameraUpdateFactory
                                                        .newLatLngZoom(new LatLng(16.0678, 108.2208), 12));
                                                                            }
                                                                        } catch (Exception e) {
                                                                            android.util.Log.e("HomeActivity", "Location update failed: " + e.getMessage());
                                            }
                                        });
                                                        }
                            } else {
                                // Yêu cầu quyền vị trí nếu chưa có
                                ActivityCompat.requestPermissions(HomeActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        LOCATION_PERMISSION_REQUEST_CODE);
                            }

                                                    // Delay load bins để tránh performance issues
                                                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                                        try {
                                                            android.util.Log.d("HomeActivity", "About to call loadBinsFromApi()");
                            loadBinsFromApi();
                                                        } catch (Exception e) {
                                                            android.util.Log.e("HomeActivity", "Load bins failed: " + e.getMessage());
                                                        }
                                                    }, 1000); // Delay 1 giây để map ổn định
                                                } catch (Exception e) {
                                                    android.util.Log.e("HomeActivity", "Map setup failed: " + e.getMessage());
                                                }
                                            }, 1000); // Delay 1 giây để tránh performance issues
                                        } catch (Exception e) {
                                            android.util.Log.e("HomeActivity", "Style loading failed: " + e.getMessage());
                                        }
                                    });
                        } catch (Exception e) {
                            android.util.Log.e("HomeActivity", "Map ready failed: " + e.getMessage());
                        }
                    }
                });
            }
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "Map async loading failed: " + e.getMessage());
        }
    }

    private void loadBinsFromApi() {
        try {
            android.util.Log.d("HomeActivity", "loadBinsFromApi() called");
            // Reset retrofit instance to try fallback URLs
            RetrofitClient.resetRetrofitInstance();
            ApiService apiService = RetrofitClient.getRetrofitInstanceWithFallback().create(ApiService.class);
            android.util.Log.d("HomeActivity", "ApiService created successfully");

            // Sử dụng endpoint mới cho Hội An
            apiService.getNearbyBinsHoiAn().enqueue(new Callback<okhttp3.ResponseBody>() {
                @Override
                public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                    try {
                        android.util.Log.d("HomeActivity", "Raw API Response Code: " + response.code());
                        if (response.isSuccessful() && response.body() != null) {
                            String responseString = response.body().string();
                            android.util.Log.d("HomeActivity", "Raw API Response Body: " + responseString);

                            // Now try to parse with the correct format
                            parseBinsResponse(responseString);
                        } else {
                            android.util.Log.w("HomeActivity", "Raw API failed, trying alternative");
                            loadBinsAlternative();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("HomeActivity", "Raw API response failed: " + e.getMessage());
                        loadBinsAlternative();
                    }
                }

                @Override
                public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                    android.util.Log.e("HomeActivity", "Raw API failed: " + t.getMessage());
                    // Check if it's a network timeout/connection issue
                    if (t.getMessage() != null && (t.getMessage().contains("timeout") || t.getMessage().contains("connect"))) {
                        android.util.Log.w("HomeActivity", "Server appears to be down, using fallback data");
                        showServerDownMessage();
                        loadFallbackBins();
                    } else {
                        loadBinsAlternative();
                    }
                }
            });
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "Load bins API setup failed: " + e.getMessage());
            showServerDownMessage();
            loadFallbackBins();
        }
    }

    private void showServerDownMessage() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Không thể kết nối đến server. Đang hiển thị dữ liệu mẫu.", Toast.LENGTH_LONG).show();
        });
    }

    private void loadFallbackBins() {
        android.util.Log.d("HomeActivity", "Loading fallback bins data");

        // Tạo một số thùng rác mẫu để demo
        List<Bin> fallbackBins = new ArrayList<>();

        // Thùng rác ở Hà Nội (khu vực trung tâm)
        fallbackBins.add(new Bin(1, "Thùng rác Cầu Giấy", 21.0285, 105.8542, "ACTIVE", 75, "Thùng rác thông minh tại Cầu Giấy"));
        fallbackBins.add(new Bin(2, "Thùng rác Ba Đình", 21.0333, 105.8333, "ACTIVE", 60, "Thùng rác thông minh tại Ba Đình"));
        fallbackBins.add(new Bin(3, "Thùng rác Hoàn Kiếm", 21.0285, 105.8542, "ACTIVE", 45, "Thùng rác thông minh tại Hoàn Kiếm"));
        fallbackBins.add(new Bin(4, "Thùng rác Đống Đa", 21.0167, 105.8333, "INACTIVE", 90, "Thùng rác thông minh tại Đống Đa"));
        fallbackBins.add(new Bin(5, "Thùng rác Hai Bà Trưng", 21.0167, 105.8500, "ACTIVE", 30, "Thùng rác thông minh tại Hai Bà Trưng"));

        // Thùng rác ở TP.HCM (khu vực trung tâm)
        fallbackBins.add(new Bin(6, "Thùng rác Quận 1", 10.7769, 106.7009, "ACTIVE", 80, "Thùng rác thông minh tại Quận 1"));
        fallbackBins.add(new Bin(7, "Thùng rác Quận 3", 10.7829, 106.6881, "ACTIVE", 55, "Thùng rác thông minh tại Quận 3"));
        fallbackBins.add(new Bin(8, "Thùng rác Quận 5", 10.7559, 106.6670, "ACTIVE", 40, "Thùng rác thông minh tại Quận 5"));
        fallbackBins.add(new Bin(9, "Thùng rác Quận 7", 10.7373, 106.7226, "INACTIVE", 95, "Thùng rác thông minh tại Quận 7"));
        fallbackBins.add(new Bin(10, "Thùng rác Quận 10", 10.7679, 106.6668, "ACTIVE", 25, "Thùng rác thông minh tại Quận 10"));

        android.util.Log.d("HomeActivity", "Created " + fallbackBins.size() + " fallback bins");

        // Hiển thị trên map
        displayBinsOnMap(fallbackBins);
    }

    private void parseBinsResponse(String responseString) {
        try {
            android.util.Log.d("HomeActivity", "Parsing response: " + responseString);

            // Try different parsing strategies based on the actual response format
            com.google.gson.Gson gson = new com.google.gson.Gson();

            // Fast-path: new server format { status, message, data: [ ... ] } with custom fields
            try {
                com.google.gson.JsonObject jsonObject = gson.fromJson(responseString, com.google.gson.JsonObject.class);
                if (jsonObject != null && jsonObject.has("data") && jsonObject.get("data").isJsonArray()) {
                    com.google.gson.JsonArray dataArray = jsonObject.getAsJsonArray("data");
                    java.util.List<Bin> bins = new java.util.ArrayList<>();
                    for (com.google.gson.JsonElement el : dataArray) {
                        if (!el.isJsonObject()) continue;
                        com.google.gson.JsonObject obj = el.getAsJsonObject();
                        Bin bin = new Bin();
                        if (obj.has("binID")) bin.setBinId(obj.get("binID").getAsInt());
                        if (obj.has("binId")) bin.setBinId(obj.get("binId").getAsInt());
                        if (obj.has("binCode")) bin.setBinCode(obj.get("binCode").getAsString());
                        if (obj.has("street")) bin.setStreet(obj.get("street").getAsString());
                        if (obj.has("latitude")) bin.setLatitude(obj.get("latitude").getAsDouble());
                        if (obj.has("longitude")) bin.setLongitude(obj.get("longitude").getAsDouble());
                        if (obj.has("capacity")) bin.setCapacity(obj.get("capacity").getAsDouble());
                        if (obj.has("currentFill")) bin.setCurrentFill(obj.get("currentFill").getAsDouble());
                        if (obj.has("status")) {
                            try {
                                String statusString = obj.get("status").isJsonPrimitive() && obj.get("status").getAsJsonPrimitive().isNumber()
                                        ? String.valueOf(obj.get("status").getAsInt())
                                        : obj.get("status").getAsString();
                                bin.setStatus(statusString);
                            } catch (Exception ignore) {}
                        }
                        if (obj.has("wardName")) {
                            bin.setWardName(obj.get("wardName").getAsString());
                        } else if (obj.has("ward") && obj.get("ward").isJsonObject()) {
                            com.google.gson.JsonObject wardObj = obj.getAsJsonObject("ward");
                            if (wardObj.has("wardName")) bin.setWardName(wardObj.get("wardName").getAsString());
                            if (wardObj.has("province") && wardObj.get("province").isJsonObject()) {
                                com.google.gson.JsonObject provObj = wardObj.getAsJsonObject("province");
                                if (provObj.has("provinceName")) bin.setProvinceName(provObj.get("provinceName").getAsString());
                            }
                        }
                        if (bin.getProvinceName() == null || bin.getProvinceName().isEmpty()) {
                            if (obj.has("provinceName")) bin.setProvinceName(obj.get("provinceName").getAsString());
                            else if (obj.has("city")) bin.setProvinceName(obj.get("city").getAsString());
                        }
                        if (obj.has("lastUpdated") && obj.get("lastUpdated").isJsonPrimitive()) {
                            try { bin.setLastUpdated(new java.util.Date(obj.get("lastUpdated").getAsLong())); } catch (Exception ignore) {}
                        }
                        bins.add(bin);
                    }
                    if (!bins.isEmpty()) {
                        android.util.Log.d("HomeActivity", "Parsed " + bins.size() + " bins (fast-path)");
                        displayBinsOnMap(bins);
                        return;
                    }
                }
            } catch (Exception e) {
                android.util.Log.d("HomeActivity", "Fast-path parse failed: " + e.getMessage());
            }

            // Strategy 1: Try parsing as server response format: {"status":"SUCCESS","data":{"bins":[...]}}
            try {
                com.google.gson.JsonObject jsonObject = gson.fromJson(responseString, com.google.gson.JsonObject.class);
                if (jsonObject.has("data")) {
                    com.google.gson.JsonElement dataElement = jsonObject.get("data");
                    if (dataElement.isJsonObject()) {
                        com.google.gson.JsonObject dataObject = dataElement.getAsJsonObject();
                        if (dataObject.has("bins")) {
                            com.google.gson.JsonElement binsElement = dataObject.get("bins");
                            if (binsElement.isJsonArray()) {
                                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Bin>>(){}.getType();
                                List<Bin> bins = gson.fromJson(binsElement, listType);
                                if (bins != null && !bins.isEmpty()) {
                                    android.util.Log.d("HomeActivity", "Successfully parsed from server data.bins array: " + bins.size() + " bins");
                                    displayBinsOnMap(bins);
                                    return;
                                }
                            }
                        }
                    } else if (dataElement.isJsonArray()) {
                        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Bin>>(){}.getType();
                        List<Bin> bins = gson.fromJson(dataElement, listType);
                        if (bins != null && !bins.isEmpty()) {
                            android.util.Log.d("HomeActivity", "Successfully parsed from server data array: " + bins.size() + " bins");
                            displayBinsOnMap(bins);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.d("HomeActivity", "Failed to parse from server data field: " + e.getMessage());
            }

            // Strategy 1b: New server format { status, message, data: [ { binID, ... } ] }
            try {
                com.google.gson.JsonObject jsonObject = gson.fromJson(responseString, com.google.gson.JsonObject.class);
                if (jsonObject.has("data") && jsonObject.get("data").isJsonArray()) {
                    com.google.gson.JsonArray dataArray = jsonObject.getAsJsonArray("data");
                    java.util.List<Bin> bins = new java.util.ArrayList<>();
                    for (com.google.gson.JsonElement el : dataArray) {
                        if (!el.isJsonObject()) continue;
                        com.google.gson.JsonObject obj = el.getAsJsonObject();

                        Bin bin = new Bin();
                        // IDs and basic fields
                        if (obj.has("binID")) bin.setBinId(obj.get("binID").getAsInt());
                        if (obj.has("binId")) bin.setBinId(obj.get("binId").getAsInt());
                        if (obj.has("binCode")) bin.setBinCode(obj.get("binCode").getAsString());
                        if (obj.has("street")) bin.setStreet(obj.get("street").getAsString());

                        // Coordinates
                        if (obj.has("latitude")) bin.setLatitude(obj.get("latitude").getAsDouble());
                        if (obj.has("longitude")) bin.setLongitude(obj.get("longitude").getAsDouble());

                        // Capacity / fill
                        if (obj.has("capacity")) bin.setCapacity(obj.get("capacity").getAsDouble());
                        if (obj.has("currentFill")) bin.setCurrentFill(obj.get("currentFill").getAsDouble());

                        // Status can be number or string
                        if (obj.has("status")) {
                            try {
                                String statusString = obj.get("status").isJsonPrimitive() && obj.get("status").getAsJsonPrimitive().isNumber()
                                        ? String.valueOf(obj.get("status").getAsInt())
                                        : obj.get("status").getAsString();
                                bin.setStatus(statusString);
                            } catch (Exception ignore) {
                                // leave default
                            }
                        }

                        // Names (ward/province/city)
                        if (obj.has("wardName")) {
                            bin.setWardName(obj.get("wardName").getAsString());
                        } else if (obj.has("ward") && obj.get("ward").isJsonObject()) {
                            com.google.gson.JsonObject wardObj = obj.getAsJsonObject("ward");
                            if (wardObj.has("wardName")) bin.setWardName(wardObj.get("wardName").getAsString());
                            if (wardObj.has("province") && wardObj.get("province").isJsonObject()) {
                                com.google.gson.JsonObject provObj = wardObj.getAsJsonObject("province");
                                if (provObj.has("provinceName")) bin.setProvinceName(provObj.get("provinceName").getAsString());
                            }
                        }
                        if (bin.getProvinceName() == null || bin.getProvinceName().isEmpty()) {
                            if (obj.has("provinceName")) {
                                bin.setProvinceName(obj.get("provinceName").getAsString());
                            } else if (obj.has("city")) {
                                bin.setProvinceName(obj.get("city").getAsString());
                            }
                        }

                        // lastUpdated as epoch millis (optional)
                        if (obj.has("lastUpdated") && obj.get("lastUpdated").isJsonPrimitive()) {
                            try {
                                long epochMs = obj.get("lastUpdated").getAsLong();
                                bin.setLastUpdated(new java.util.Date(epochMs));
                            } catch (Exception ignore) {
                            }
                        }

                        bins.add(bin);
                    }

                    if (!bins.isEmpty()) {
                        android.util.Log.d("HomeActivity", "Parsed " + bins.size() + " bins from new 'data' array format");
                        displayBinsOnMap(bins);
                        return;
                    }
                }
            } catch (Exception e) {
                android.util.Log.d("HomeActivity", "Failed to parse new server data array format: " + e.getMessage());
            }

            // Strategy 2: Try parsing as direct array
            try {
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Bin>>(){}.getType();
                List<Bin> bins = gson.fromJson(responseString, listType);
                if (bins != null && !bins.isEmpty()) {
                    android.util.Log.d("HomeActivity", "Successfully parsed as direct array: " + bins.size() + " bins");
                    displayBinsOnMap(bins);
                    return;
                }
            } catch (Exception e) {
                android.util.Log.d("HomeActivity", "Failed to parse as direct array: " + e.getMessage());
            }

            // Strategy 3: Try parsing as wrapped object with items field
            try {
                com.google.gson.JsonObject jsonObject = gson.fromJson(responseString, com.google.gson.JsonObject.class);
                if (jsonObject.has("items")) {
                    com.google.gson.JsonElement itemsElement = jsonObject.get("items");
                    if (itemsElement.isJsonArray()) {
                        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Bin>>(){}.getType();
                        List<Bin> bins = gson.fromJson(itemsElement, listType);
                        if (bins != null && !bins.isEmpty()) {
                            android.util.Log.d("HomeActivity", "Successfully parsed from items array: " + bins.size() + " bins");
                            displayBinsOnMap(bins);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.d("HomeActivity", "Failed to parse from items field: " + e.getMessage());
            }

            // Strategy 4: Try parsing as wrapped object with results field
            try {
                com.google.gson.JsonObject jsonObject = gson.fromJson(responseString, com.google.gson.JsonObject.class);
                if (jsonObject.has("results")) {
                    com.google.gson.JsonElement resultsElement = jsonObject.get("results");
                    if (resultsElement.isJsonArray()) {
                        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Bin>>(){}.getType();
                        List<Bin> bins = gson.fromJson(resultsElement, listType);
                        if (bins != null && !bins.isEmpty()) {
                            android.util.Log.d("HomeActivity", "Successfully parsed from results array: " + bins.size() + " bins");
                            displayBinsOnMap(bins);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.d("HomeActivity", "Failed to parse from results field: " + e.getMessage());
            }

            android.util.Log.w("HomeActivity", "All parsing strategies failed, trying alternative API");
            loadBinsAlternative();

        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "Parse bins response failed: " + e.getMessage());
            loadBinsAlternative();
        }
    }

    private void loadBinsAlternative() {
        try {
            ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

            // Sử dụng endpoint mới cho Hội An
            apiService.getNearbyBinsHoiAn().enqueue(new Callback<okhttp3.ResponseBody>() {
                @Override
                public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                    try {
                        android.util.Log.d("HomeActivity", "Alternative API Response Code: " + response.code());
                        if (response.isSuccessful() && response.body() != null) {
                            String responseString = response.body().string();
                            android.util.Log.d("HomeActivity", "Alternative API Response Body: " + responseString);
                            parseBinsResponse(responseString);
                        } else {
                            android.util.Log.w("HomeActivity", "Alternative API failed, using fallback data");
                            showServerDownMessage();
                            loadFallbackBins();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("HomeActivity", "Load bins alternative response failed: " + e.getMessage());
                        showServerDownMessage();
                        loadFallbackBins();
                    }
                }

                @Override
                public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                    android.util.Log.e("HomeActivity", "Load bins alternative failed: " + t.getMessage());
                    // Check if it's a network timeout/connection issue
                    if (t.getMessage() != null && (t.getMessage().contains("timeout") || t.getMessage().contains("connect"))) {
                        android.util.Log.w("HomeActivity", "Server appears to be down, using fallback data");
                        showServerDownMessage();
                        loadFallbackBins();
                    } else {
                        showServerDownMessage();
                        loadFallbackBins();
                    }
                }
            });
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "Load bins alternative API setup failed: " + e.getMessage());
        }
    }

    private void loadBinsDirect() {
        try {
            ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

            // Try wrapped API response first (handles object format)
            apiService.getAllBinsWrapped().enqueue(new Callback<com.example.smartbinapp.model.ApiResponse<com.example.smartbinapp.model.Bin>>() {
                @Override
                public void onResponse(Call<com.example.smartbinapp.model.ApiResponse<com.example.smartbinapp.model.Bin>> call, Response<com.example.smartbinapp.model.ApiResponse<com.example.smartbinapp.model.Bin>> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            com.example.smartbinapp.model.ApiResponse<com.example.smartbinapp.model.Bin> apiResponse = response.body();

                            List<com.example.smartbinapp.model.Bin> bins = null;
                            if (apiResponse.getData() != null) {
                                bins = new java.util.ArrayList<>();
                                bins.add(apiResponse.getData());
                            } else if (apiResponse.getItems() != null && !apiResponse.getItems().isEmpty()) {
                                bins = apiResponse.getItems();
                            } else if (apiResponse.getResults() != null && !apiResponse.getResults().isEmpty()) {
                                bins = apiResponse.getResults();
                            }

                            if (bins != null && !bins.isEmpty()) {
                                displayBinsOnMap(bins);
                            } else {
                                // Fallback to direct API call
                                loadBinsDirectFallback();
                            }
                        } else {
                            // Fallback to direct API call
                            loadBinsDirectFallback();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("HomeActivity", "Load bins direct response failed: " + e.getMessage());
                        // Fallback to direct API call
                        loadBinsDirectFallback();
                    }
                }

                @Override
                public void onFailure(Call<com.example.smartbinapp.model.ApiResponse<com.example.smartbinapp.model.Bin>> call, Throwable t) {
                    android.util.Log.e("HomeActivity", "Load bins direct failed: " + t.getMessage());
                    // Check if it's a network timeout/connection issue
                    if (t.getMessage() != null && (t.getMessage().contains("timeout") || t.getMessage().contains("connect"))) {
                        android.util.Log.w("HomeActivity", "Server appears to be down, using fallback data");
                        showServerDownMessage();
                        loadFallbackBins();
                    } else {
                        // Fallback to direct API call
                        loadBinsDirectFallback();
                    }
                }
            });
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "Load bins direct API setup failed: " + e.getMessage());
        }
    }

    private void loadBinsDirectFallback() {
        try {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

            // Use raw response to handle server format
            apiService.getAllBinsRaw().enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
                public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                    try {
                        android.util.Log.d("HomeActivity", "Direct fallback API Response Code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                            String responseString = response.body().string();
                            android.util.Log.d("HomeActivity", "Direct fallback API Response Body: " + responseString);

                            // Parse the response using our smart parser
                            parseBinsResponse(responseString);
                        } else {
                            Toast.makeText(HomeActivity.this,
                                "Không thể tải dữ liệu thùng rác từ server.",
                                Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("HomeActivity", "Load bins direct fallback response failed: " + e.getMessage());
                        Toast.makeText(HomeActivity.this,
                            "Lỗi khi xử lý dữ liệu thùng rác.",
                            Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                    android.util.Log.e("HomeActivity", "Load bins direct fallback failed: " + t.getMessage());
                    // Check if it's a network timeout/connection issue
                    if (t.getMessage() != null && (t.getMessage().contains("timeout") || t.getMessage().contains("connect"))) {
                        android.util.Log.w("HomeActivity", "Server appears to be down, using fallback data");
                        showServerDownMessage();
                        loadFallbackBins();
                    } else {
                        Toast.makeText(HomeActivity.this,
                            "Lỗi kết nối: " + t.getMessage(),
                            Toast.LENGTH_LONG).show();
                    }
                }
            });
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "Load bins direct fallback API setup failed: " + e.getMessage());
        }
    }

    private void displayBinsOnMap(List<Bin> bins) {
        try {
            android.util.Log.d("HomeActivity", "DisplayBinsOnMap called with " + bins.size() + " bins");

            // Clear existing markers
            if (vietmapGL != null) {
                vietmapGL.clear();
                android.util.Log.d("HomeActivity", "Cleared existing markers");
            }

            // Process bins on main thread but with delay to avoid blocking
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    android.util.Log.d("HomeActivity", "Processing " + bins.size() + " bins for display");
                    for (Bin bin : bins) {
                        // Kiểm tra null và tránh chia cho 0
                        double capacity = bin.getCapacity() > 0 ? bin.getCapacity() : 1;
                        int percent = (int) ((bin.getCurrentFill() / capacity) * 100);

                        LatLng position = new LatLng(bin.getLatitude(), bin.getLongitude());
                        android.util.Log.d("HomeActivity", "Creating marker for bin: " + bin.getBinCode() + " at " + bin.getLatitude() + ", " + bin.getLongitude());

                        String binCode = bin.getBinCode() != null ? bin.getBinCode() : "Unknown";
                        String title = binCode + " - " + percent + "% đầy";

                        String street = bin.getStreet() != null ? bin.getStreet() : "";
                        String wardName = bin.getWardName() != null ? bin.getWardName() : "";
                        String provinceName = bin.getProvinceName() != null ? bin.getProvinceName() : "";
                        String snippet = street + ", " + wardName + ", " + provinceName;

                        // Chọn icon theo mức đầy
                        int iconRes;
                        if (percent > 80) {
                            iconRes = R.drawable.ic_bin_red; // thùng đầy
                        } else if (percent > 40) {
                            iconRes = R.drawable.ic_bin_yellow; // thùng trung bình
                        } else {
                            iconRes = R.drawable.ic_bin_green; // thùng trống
                        }

                        if (vietmapGL != null) {
                            try {
                        vietmapGL.addMarker(new MarkerOptions()
                                .position(position)
                                .title(title)
                                .snippet(snippet)
                                .icon(IconFactory.getInstance(HomeActivity.this)
                                        .fromBitmap(getBitmapFromVectorDrawable(iconRes))));
                                android.util.Log.d("HomeActivity", "Successfully added marker for bin: " + binCode);
                            } catch (Exception e) {
                                android.util.Log.e("HomeActivity", "Failed to add marker for bin " + binCode + ": " + e.getMessage());
                            }
                        } else {
                            android.util.Log.w("HomeActivity", "VietMapGL is null, cannot add marker");
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.e("HomeActivity", "Display bins processing failed: " + e.getMessage());
                }
            }, 100); // Delay 100ms để tránh blocking main thread

            if (vietmapGL != null) {
                try {
                    vietmapGL.setOnMarkerClickListener(marker -> {
                        try {
                        Toast.makeText(HomeActivity.this,
                                marker.getTitle() + "\n" + marker.getSnippet(),
                                Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            android.util.Log.e("HomeActivity", "Marker click failed: " + e.getMessage());
                        }
                        return false;
                    });

                    // Zoom vào thùng đầu tiên nếu có
                    if (!bins.isEmpty()) {
                        try {
                            Bin firstBin = bins.get(0);
                            if (firstBin != null) {
                                android.util.Log.d("HomeActivity", "Animating camera to bin location: " + firstBin.getLatitude() + ", " + firstBin.getLongitude());
                        vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(firstBin.getLatitude(), firstBin.getLongitude()), 14));
                                android.util.Log.d("HomeActivity", "Camera animation started");
                            }
                        } catch (Exception e) {
                            android.util.Log.e("HomeActivity", "Camera animation to bin failed: " + e.getMessage());
                            // Fallback to Đà Nẵng
                            try {
                                android.util.Log.d("HomeActivity", "Falling back to Đà Nẵng location");
                                vietmapGL.animateCamera(CameraUpdateFactory
                                        .newLatLngZoom(new LatLng(16.0678, 108.2208), 12));
                            } catch (Exception ex) {
                                android.util.Log.e("HomeActivity", "Camera animation failed: " + ex.getMessage());
                            }
                    }
                } else {
                        android.util.Log.w("HomeActivity", "No bins to zoom to");
                    }
                } catch (Exception e) {
                    android.util.Log.e("HomeActivity", "Map interaction setup failed: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "Display bins on map failed: " + e.getMessage());
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent() {
        try {
            android.util.Log.d("HomeActivity", "Attempting to enable location component");

            // Check both fine and coarse location permissions
            boolean hasFineLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean hasCoarseLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            if (!hasFineLocation && !hasCoarseLocation) {
                android.util.Log.w("HomeActivity", "No location permissions granted, requesting...");
            ActivityCompat.requestPermissions(this,
                        new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

            if (vietmapGL != null) {
                try {
                    android.util.Log.d("HomeActivity", "Getting location component from VietMapGL");
        // Lấy LocationComponent
        LocationComponent locationComponent = vietmapGL.getLocationComponent();

                    // Tạo options để kích hoạt với custom drawables
        LocationComponentActivationOptions options =
                LocationComponentActivationOptions.builder(this, vietmapGL.getStyle())
                        .useDefaultLocationEngine(true)
                        .build();

                    android.util.Log.d("HomeActivity", "Activating location component");
        // Kích hoạt
        locationComponent.activateLocationComponent(options);

                    android.util.Log.d("HomeActivity", "Setting location component enabled");
        // Hiển thị vị trí hiện tại
        locationComponent.setLocationComponentEnabled(true);

                    android.util.Log.d("HomeActivity", "Setting camera mode to tracking");
        // Camera tracking user location
        locationComponent.setCameraMode(CameraMode.TRACKING);

                    android.util.Log.d("HomeActivity", "Setting render mode to normal");
                    // Sử dụng RenderMode.NORMAL thay vì COMPASS để tránh lỗi drawable
                    locationComponent.setRenderMode(RenderMode.NORMAL);

                    android.util.Log.d("HomeActivity", "Location component enabled successfully");

                } catch (Exception e) {
                    android.util.Log.e("HomeActivity", "Location component activation failed: " + e.getMessage());
                    // Fallback: chỉ enable location mà không set render mode
                    try {
                        android.util.Log.d("HomeActivity", "Trying fallback location component setup");
                        LocationComponent locationComponent = vietmapGL.getLocationComponent();
                        locationComponent.setLocationComponentEnabled(true);
                        locationComponent.setCameraMode(CameraMode.TRACKING);
                        android.util.Log.d("HomeActivity", "Fallback location component setup successful");
                    } catch (Exception ex) {
                        android.util.Log.e("HomeActivity", "Location component fallback failed: " + ex.getMessage());
                        // Show user-friendly message
                        Toast.makeText(this, "Không thể kích hoạt định vị. Vui lòng kiểm tra quyền truy cập vị trí.", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                android.util.Log.w("HomeActivity", "VietMapGL is null, cannot enable location component");
            }
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "Enable location component failed: " + e.getMessage());
            Toast.makeText(this, "Lỗi kích hoạt định vị: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initializeViews() {
        ivMenu = findViewById(R.id.iv_menu);
        btnHome = findViewById(R.id.btn_home);
        btnShowTask = findViewById(R.id.btn_showtask);
        btnAccount = findViewById(R.id.btn_account);
        fabReport = findViewById(R.id.fab_report);
        fabMenu = findViewById(R.id.fab_menu);
    }

    private void startEntranceAnimations() {
        // Delay animations để tránh performance issues
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
        ObjectAnimator animator = ObjectAnimator.ofFloat(findViewById(R.id.top_bar), "translationY", -100f, 0f);
            animator.setDuration(300); // Giảm duration xuống 300ms
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();

        ObjectAnimator bottomNavAnimator = ObjectAnimator.ofFloat(findViewById(R.id.bottom_navigation),
                "translationY", 100f, 0f);
            bottomNavAnimator.setDuration(300); // Giảm duration xuống 300ms
            bottomNavAnimator.setStartDelay(50);
        bottomNavAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        bottomNavAnimator.start();

        ObjectAnimator fabScaleX = ObjectAnimator.ofFloat(fabReport, "scaleX", 0f, 1f);
        ObjectAnimator fabScaleY = ObjectAnimator.ofFloat(fabReport, "scaleY", 0f, 1f);
            fabScaleX.setDuration(200); // Giảm duration xuống 200ms
            fabScaleY.setDuration(200);
            fabScaleX.setStartDelay(200); // Giảm delay
            fabScaleY.setStartDelay(200);
        fabScaleX.start();
        fabScaleY.start();

            // Animation for menu button
            ObjectAnimator menuScaleX = ObjectAnimator.ofFloat(fabMenu, "scaleX", 0f, 1f);
            ObjectAnimator menuScaleY = ObjectAnimator.ofFloat(fabMenu, "scaleY", 0f, 1f);
            menuScaleX.setDuration(200); // Giảm duration xuống 200ms
            menuScaleY.setDuration(200);
            menuScaleX.setStartDelay(300); // Giảm delay
            menuScaleY.setStartDelay(300);
            menuScaleX.start();
            menuScaleY.start();
        }, 500); // Delay 500ms để tránh performance issues
    }


    private void showMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Menu");

        String[] options = {
            "Thùng rác gần nhất",
            "Báo cáo của tôi",
            "Đánh giá dịch vụ"
        };

        builder.setItems(options, (dialog, which) -> {
            Intent intent = null;
            switch (which) {
                case 0: // Thùng rác gần nhất
                    intent = new Intent(HomeActivity.this, NearbyBinsActivity.class);
                    break;
                case 1: // Báo cáo của tôi
                    intent = new Intent(HomeActivity.this, ReportsListActivity.class);
                    break;
                case 2: // Đánh giá dịch vụ
                    intent = new Intent(HomeActivity.this, FeedbackActivity.class);
                    break;
            }

            if (intent != null) {
                startActivity(intent);
            }
        });

        builder.show();
    }

    private void setupClickListeners() {
        ivMenu.setOnClickListener(v -> {
            animateButtonClick(v);
            showMenu();
        });

        btnHome.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, true);
            setActiveTab(btnShowTask, false);
            setActiveTab(btnAccount, false);
        });

        btnShowTask.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, false);
            setActiveTab(btnShowTask, true);
            setActiveTab(btnAccount, false);
            Intent intent = new Intent(HomeActivity.this, ReportsListActivity.class);
            startActivity(intent);
        });

        btnAccount.setOnClickListener(v -> {
            animateButtonClick(v);
            setActiveTab(btnHome, false);
            setActiveTab(btnShowTask, false);
            setActiveTab(btnAccount, true);
            Intent intent = new Intent(HomeActivity.this, com.example.smartbinapp.ProfileActivity.class);
            startActivity(intent);
            finish();
        });

        fabReport.setOnClickListener(v -> {
            animateButtonClick(v);
            Intent intent = new Intent(HomeActivity.this, NearbyBinsActivity.class);
            startActivity(intent);
        });

        fabMenu.setOnClickListener(v -> {
            animateButtonClick(v);
            showMenu();
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
        try {
        Drawable drawable = ContextCompat.getDrawable(this, drawableId);
        if (drawable == null) return null;

            // Use smaller bitmap size to reduce memory usage
            int size = 32; // Giảm từ 48 xuống 32 để tiết kiệm memory
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565); // Sử dụng RGB_565 thay vì ARGB_8888
        Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);
        return bitmap;
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "Bitmap creation failed: " + e.getMessage());
            return null;
        }
    }

    // Lifecycle MapView với error handling
    @Override
    protected void onStart() {
        super.onStart();
        try {
            if (mapView != null) {
                mapView.onStart();
            }
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "MapView onStart failed: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (mapView != null) {
                mapView.onResume();
            }
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "MapView onResume failed: " + e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (mapView != null) {
                mapView.onPause();
            }
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "MapView onPause failed: " + e.getMessage());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            if (mapView != null) {
                mapView.onStop();
            }
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "MapView onStop failed: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mapView != null) {
                mapView.onDestroy();
            }
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "MapView onDestroy failed: " + e.getMessage());
        }

        // Clear references to prevent memory leaks
        vietmapGL = null;
        mapView = null;
        fusedLocationClient = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            if (mapView != null) {
        mapView.onSaveInstanceState(outState);
            }
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "MapView onSaveInstanceState failed: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                android.util.Log.d("HomeActivity", "Location permissions granted, enabling location component");
                enableLocationComponent();
            } else {
                android.util.Log.w("HomeActivity", "Location permissions denied");
                Toast.makeText(this, "Cần quyền vị trí để hiển thị thùng rác gần nhất", Toast.LENGTH_LONG).show();
            }
        }
    }
}