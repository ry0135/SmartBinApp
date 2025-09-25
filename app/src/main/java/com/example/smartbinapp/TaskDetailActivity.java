package com.example.smartbinapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartbinapp.model.ApiMessage;
import com.example.smartbinapp.model.Task;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;

import vn.vietmap.vietmapsdk.Vietmap;
import vn.vietmap.vietmapsdk.annotations.IconFactory;
import vn.vietmap.vietmapsdk.annotations.Marker;
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

public class TaskDetailActivity extends AppCompatActivity {

    // ====== CONSTANTS ======
    private static final String TAG = "TaskDetail";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 20;
    private static final float STEP_TRIGGER_METERS = 30f; // đổi 5–10m để test đứng yên
    private static final String VMAP_API_KEY = "ecdbd35460b2d399e18592e6264186757aaaddd8755b774c"; // TODO: thay bằng API key Vietmap thật
    private static final String FILE_PROVIDER_AUTH = BuildConfig.APPLICATION_ID + ".provider"; // TODO: khai báo provider trong Manifest

    // ====== MAP ======
    private MapView mapView;
    private VietMapGL vietmapGL;

    // ====== USER/BATCH ======
    private int workerId;
    private String batchId;

    // ====== DATA & UI ======
    private final List<Task> allTasks = new ArrayList<>();
    private final Map<Marker, Task> markerTaskMap = new HashMap<>();
    private Polyline currentRoute;

    // ====== ROUTE / NAV ======
    private final List<LatLng> polylinePoints = new ArrayList<>();
    private final List<JSONObject> routeSteps = new ArrayList<>();
    private int currentStepGlobal = 0; // dùng cho followRouteWithVoice

    // ====== TTS ======
    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;

    // ====== LOCATION ======
    private FusedLocationProviderClient fusedClient;
    private com.google.android.gms.location.LocationCallback navCallback;
    private boolean isNavigating = false;

    // ====== CAMERA / PROOF ======
    private Uri photoUri;
    private File tempPhotoFile;
    private Task currentTaskToComplete;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && currentTaskToComplete != null) {
                    File imageFile = (tempPhotoFile != null && tempPhotoFile.exists())
                            ? tempPhotoFile
                            : (photoUri != null ? copyUriToCache(photoUri) : null);

                    if (imageFile != null && imageFile.exists()) {
                        uploadProof(currentTaskToComplete, imageFile);
                    } else {
                        Toast.makeText(this, "Không đọc được ảnh chụp", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Đã hủy chụp ảnh", Toast.LENGTH_SHORT).show();
                }
            });

    // ====== LIFECYCLE ======
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Vietmap.getInstance(this);
        setContentView(R.layout.activity_task_detail);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        ExtendedFloatingActionButton fabOptimize = findViewById(R.id.btnOptimize);
        ExtendedFloatingActionButton fabStart = findViewById(R.id.btnStartCollect);

        fabOptimize.setOnClickListener(v -> {
            if (allTasks.isEmpty()) {
                Toast.makeText(this, "Không có điểm để tối ưu", Toast.LENGTH_SHORT).show();
            } else {
                drawRouteOnly(allTasks);
            }
        });
        fabStart.setOnClickListener(v -> startCollectingRoute());

        // TTS
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int res = textToSpeech.setLanguage(Locale.forLanguageTag("vi-VN"));
                ttsReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

        // User / Batch
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String savedUserId = prefs.getString("userId", "0");
        workerId = (savedUserId != null) ? Integer.parseInt(savedUserId) : 0;

        batchId = getIntent().getStringExtra("batchId");
        if (batchId == null) {
            Toast.makeText(this, "Không có batchId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mapView.getMapAsync(map -> {
            vietmapGL = map;
            vietmapGL.setStyle(
                    new Style.Builder().fromUri("https://maps.vietmap.vn/api/maps/light/styles.json?apikey=" + VMAP_API_KEY),
                    style -> {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {
                            enableLocationComponent();
                        } else {
                            ActivityCompat.requestPermissions(
                                    this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    LOCATION_PERMISSION_REQUEST_CODE
                            );
                        }
                        loadTasksFromApi();
                    }
            );
        });

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
    }

    // ====== LOAD TASKS & MARKERS ======
    private void loadTasksFromApi() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.getTasksInBatch(workerId, batchId).enqueue(new retrofit2.Callback<List<Task>>() {
            @Override
            public void onResponse(retrofit2.Call<List<Task>> call, retrofit2.Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allTasks.clear();
                    allTasks.addAll(response.body());

                    for (Task task : allTasks) {
                        LatLng pos = new LatLng(task.getBin().getLatitude(), task.getBin().getLongitude());
                        int iconRes = getStatusIcon(task.getStatus());

                        Marker marker = vietmapGL.addMarker(new MarkerOptions()
                                .position(pos)
                                .title("Bin " + task.getBin().getBinCode() + " (" + task.getTaskType() + ")")
                                .snippet("Trạng thái: " + task.getStatus())
                                .icon(IconFactory.getInstance(TaskDetailActivity.this)
                                        .fromBitmap(getBitmapFromVectorDrawable(iconRes))));

                        markerTaskMap.put(marker, task);
                    }

                    if (!allTasks.isEmpty()) {
                        Task first = allTasks.get(0);
                        vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(first.getBin().getLatitude(), first.getBin().getLongitude()), 15));
                    }

                    vietmapGL.setOnMarkerClickListener(marker -> {
                        Task clickedTask = markerTaskMap.get(marker);
                        if (clickedTask != null) showBinDetailBottomSheet(clickedTask);
                        return true;
                    });
                } else {
                    Toast.makeText(TaskDetailActivity.this, "Không tải được danh sách thùng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<Task>> call, Throwable t) {
                Toast.makeText(TaskDetailActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ====== DRAW ROUTE ONLY ======
    @SuppressWarnings({"MissingPermission"})
    private void drawRouteOnly(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            Toast.makeText(this, "Không có điểm để tối ưu", Toast.LENGTH_SHORT).show();
            return;
        }
        LocationComponent lc = vietmapGL.getLocationComponent();
        Location myLocation = (lc != null) ? lc.getLastKnownLocation() : null;
        if (myLocation == null) {
            Toast.makeText(this, "Không lấy được vị trí hiện tại", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder url = new StringBuilder("https://maps.vietmap.vn/api/route")
                .append("?api-version=1.1")
                .append("&apikey=").append(VMAP_API_KEY)
                .append("&vehicle=car")
                .append("&points_encoded=false")
                .append("&instructions=true");

        url.append("&point=").append(myLocation.getLatitude()).append(",").append(myLocation.getLongitude());
        for (Task t : tasks) {
            url.append("&point=").append(t.getBin().getLatitude()).append(",").append(t.getBin().getLongitude());
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url.toString()).build();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(TaskDetailActivity.this, "API lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
            @Override public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() -> Toast.makeText(TaskDetailActivity.this, "API trả lỗi: " + response.code(), Toast.LENGTH_SHORT).show());
                    return;
                }
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    JSONArray paths = json.optJSONArray("paths");
                    if (paths == null || paths.length() == 0) {
                        runOnUiThread(() -> Toast.makeText(TaskDetailActivity.this, "Không tìm thấy tuyến phù hợp", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    JSONObject path = paths.getJSONObject(0);

                    // Polyline points
                    JSONArray coords = path.getJSONObject("points").getJSONArray("coordinates");
                    polylinePoints.clear();
                    for (int i = 0; i < coords.length(); i++) {
                        JSONArray c = coords.getJSONArray(i); // [lng, lat]
                        polylinePoints.add(new LatLng(c.getDouble(1), c.getDouble(0)));
                    }

                    // Instructions
                    routeSteps.clear();
                    JSONArray instructions = path.optJSONArray("instructions");
                    if (instructions != null) {
                        for (int i = 0; i < instructions.length(); i++) {
                            routeSteps.add(instructions.getJSONObject(i));
                        }
                    }
                    currentStepGlobal = 0;

                    runOnUiThread(() -> {
                        if (currentRoute != null) vietmapGL.removeAnnotation(currentRoute);
                        currentRoute = vietmapGL.addPolyline(new PolylineOptions().addAll(polylinePoints).color(Color.BLUE).width(5f));
                        if (!polylinePoints.isEmpty()) {
                            vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(polylinePoints.get(0), 15f));
                        }
                        Toast.makeText(TaskDetailActivity.this, "Đã vẽ tuyến tối ưu", Toast.LENGTH_SHORT).show();
                    });

                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(TaskDetailActivity.this, "Parse JSON lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // ====== START / FOLLOW ROUTE WITH VOICE ======
    private void startCollectingRoute() {
        if (polylinePoints.isEmpty() || routeSteps.isEmpty()) {
            Toast.makeText(this, "Chưa có tuyến để theo dõi, hãy bấm Tối ưu trước!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ttsReady) {
            Toast.makeText(this, "TTS chưa sẵn sàng để đọc", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isNavigating) {
            Toast.makeText(this, "Đang điều hướng...", Toast.LENGTH_SHORT).show();
            return;
        }
        followRouteWithVoice();
        Toast.makeText(this, "Bắt đầu thu gom!", Toast.LENGTH_SHORT).show();
    }

    @SuppressWarnings({"MissingPermission"})
    private void followRouteWithVoice() {
        if (fusedClient == null) fusedClient = LocationServices.getFusedLocationProviderClient(this);

        com.google.android.gms.location.LocationRequest request =
                com.google.android.gms.location.LocationRequest.create()
                        .setInterval(2000)
                        .setFastestInterval(1000)
                        .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY);

        navCallback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult locationResult) {
                if (locationResult == null || routeSteps.isEmpty()) return;

                Location loc = locationResult.getLastLocation();
                if (loc == null) return;

                double lat = loc.getLatitude();
                double lng = loc.getLongitude();

                try {
                    if (currentStepGlobal < routeSteps.size()) {
                        JSONObject step = routeSteps.get(currentStepGlobal);
                        JSONArray interval = step.getJSONArray("interval"); // [startIdx, endIdx]
                        int endIdx = interval.getInt(1);
                        if (endIdx < 0 || endIdx >= polylinePoints.size()) {
                            currentStepGlobal++;
                            return;
                        }
                        LatLng target = polylinePoints.get(endIdx);

                        float[] d = new float[1];
                        Location.distanceBetween(lat, lng, target.getLatitude(), target.getLongitude(), d);

                        String text = step.optString("text", "Tiếp tục theo tuyến đường");

                        if (currentStepGlobal == 0) {
                            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "STEP_0");
                            currentStepGlobal++;
                        } else if (d[0] <= STEP_TRIGGER_METERS) {
                            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, "STEP_" + currentStepGlobal);
                            currentStepGlobal++;
                        }

                        if (currentStepGlobal >= routeSteps.size()) {
                            textToSpeech.speak("Đã đến điểm cuối lộ trình", TextToSpeech.QUEUE_ADD, null, "DONE");
                            stopNavigationUpdates();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "NAV error: " + e.getMessage());
                }
            }
        };

        fusedClient.requestLocationUpdates(request, navCallback, getMainLooper());
        isNavigating = true;
    }

    private void stopNavigationUpdates() {
        if (fusedClient != null && navCallback != null) {
            fusedClient.removeLocationUpdates(navCallback);
        }
        isNavigating = false;
    }

    // ====== BOTTOM SHEET ======
    private void showBinDetailBottomSheet(Task task) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_bottomsheet_bin_detail, null);
        dialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tvBinTitle);
        TextView tvStatus = view.findViewById(R.id.tvBinStatus);
        Button btnComplete = view.findViewById(R.id.btnCompleteBin);

        tvTitle.setText("Bin " + task.getBin().getBinCode() + " (" + task.getTaskType() + ")");
        tvStatus.setText("Trạng thái: " + task.getStatus());

        btnComplete.setOnClickListener(v -> {
            dialog.dismiss();
            completeTaskWithProof(task);
        });
        dialog.show();
    }

    // ====== CAMERA & UPLOAD ======
    private void completeTaskWithProof(Task task) {
        currentTaskToComplete = task;

        // Tạo file tạm để lưu ảnh full-res
        try {
            tempPhotoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTH, tempPhotoFile);
        } catch (IOException e) {
            Toast.makeText(this, "Không tạo được file ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(cameraIntent);
        } else {
            Toast.makeText(this, "Thiết bị không có ứng dụng Camera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "proof_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    private File copyUriToCache(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            File outFile = File.createTempFile("proof_copy_", ".jpg", getCacheDir());
            try (FileOutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                return outFile;
            }
        } catch (Exception e) {
            Log.e(TAG, "copyUriToCache error: " + e.getMessage());
            return null;
        }
    }

    private void uploadProof(Task task, File imageFile) {
        // Kiểm tra quyền vị trí
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            Toast.makeText(this, "Cần quyền vị trí để gửi báo cáo", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedClient.getLastLocation()
                .addOnSuccessListener(TaskDetailActivity.this, location -> {
                    if (location == null) {
                        Toast.makeText(this, "Không lấy được GPS", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    RequestBody imgBody = RequestBody.create(imageFile, MediaType.parse("image/*"));
                    MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", imageFile.getName(), imgBody);

                    RequestBody latBody = RequestBody.create(String.valueOf(lat), MediaType.parse("text/plain"));
                    RequestBody lngBody = RequestBody.create(String.valueOf(lng), MediaType.parse("text/plain"));
                    RequestBody taskIdBody = RequestBody.create(String.valueOf(task.getTaskID()), MediaType.parse("text/plain"));

                    ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
                    Call<ApiMessage> call = apiService.completeTask(taskIdBody, latBody, lngBody, imagePart);

                    call.enqueue(new Callback<ApiMessage>() {
                        @Override
                        public void onResponse(Call<ApiMessage> call, retrofit2.Response<ApiMessage> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Toast.makeText(TaskDetailActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(TaskDetailActivity.this, "API lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<ApiMessage> call, Throwable t) {
                            Toast.makeText(TaskDetailActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi GPS: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ====== HELPERS ======
    private int getStatusIcon(String status) {
        if (status == null) return R.drawable.ic_bin_red;
        switch (status.toUpperCase()) {
            case "COMPLETED": return R.drawable.ic_bin_green;
            case "DOING": return R.drawable.ic_bin_yellow;
            default: return R.drawable.ic_bin_red;
        }
    }

    private Bitmap getBitmapFromVectorDrawable(int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableId);
        if (drawable == null) {
            Bitmap empty = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            empty.eraseColor(Color.TRANSPARENT);
            return empty;
        }
        Bitmap bitmap = Bitmap.createBitmap(
                Math.max(1, drawable.getIntrinsicWidth()),
                Math.max(1, drawable.getIntrinsicHeight()),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
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
                        .useDefaultLocationEngine(true)
                        .locationComponentOptions(customOptions)
                        .build();

        locationComponent.activateLocationComponent(options);
        locationComponent.setLocationComponentEnabled(true);
        locationComponent.setCameraMode(CameraMode.TRACKING);
        locationComponent.setRenderMode(RenderMode.NORMAL);
    }

    // ====== PERMISSIONS & LIFECYCLE ======
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationComponent();
            } else {
                Toast.makeText(this, "Cần cấp quyền vị trí", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() {
        super.onPause();
        mapView.onPause();
        stopNavigationUpdates();
    }
    @Override protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override protected void onDestroy() {
        stopNavigationUpdates();
        if (textToSpeech != null) { textToSpeech.stop(); textToSpeech.shutdown(); }
        super.onDestroy();
    }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
