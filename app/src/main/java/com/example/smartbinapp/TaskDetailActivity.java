package com.example.smartbinapp;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import vn.vietmap.vietmapsdk.annotations.Icon;
import vn.vietmap.vietmapsdk.annotations.IconFactory;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.bumptech.glide.Glide;
import com.example.smartbinapp.model.ApiMessage;
import com.example.smartbinapp.model.Bin;
import com.example.smartbinapp.model.Task;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;
import com.example.smartbinapp.service.BinWebSocketService;
import com.example.smartbinapp.service.TaskWebSocketService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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

import okhttp3.OkHttpClient;
import okhttp3.Request;


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

    // Realtime WebSocket
    private final BinWebSocketService wsService = new BinWebSocketService();

    // Cache icons and markers (GIỐNG HOME)
    private Bitmap iconRed, iconYellow, iconGreen, iconGrey, iconDefault;

    // Map để lưu dữ liệu bin cho mỗi marker
    private final Map<Marker, Task> binDataMap = new HashMap<>();

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

    private boolean isCollecting = false;
    private TaskWebSocketService taskWebSocketService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Vietmap.getInstance(this);
        setContentView(R.layout.activity_task_detail);
        initIcons();
        wsService.connect();
        wsService.setListener(this::onBinUpdateReceived);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        ExtendedFloatingActionButton fabOptimize = findViewById(R.id.btnOptimize);
        ExtendedFloatingActionButton fabStart = findViewById(R.id.btnStartCollect);
        ExtendedFloatingActionButton fabReportBatch = findViewById(R.id.btnReportBatch);
        fabReportBatch.setOnClickListener(v -> showReportDialog(true, null));
        fabOptimize.setOnClickListener(v -> {
            if (allTasks.isEmpty()) {
                Toast.makeText(this, "Không có điểm để tối ưu", Toast.LENGTH_SHORT).show();
            } else {
                drawRouteOnly(allTasks);
            }
        });
        fabStart.setOnClickListener(v -> {

            // ⭐ Nếu có task OPEN → không cho thu gom
            boolean hasOpenTask = false;
            for (Task t : allTasks) {
                if ("OPEN".equalsIgnoreCase(t.getStatus())) {
                    hasOpenTask = true;
                    break;
                }
            }

            if (hasOpenTask) {
                Toast.makeText(this, "Bạn cần nhận nhiệm vụ trước!", Toast.LENGTH_SHORT).show();
                return;  // ❌ không cho chạy tiếp
            }

            // ⭐ Xử lý thu gom
            if (!isCollecting) {
                currentStepGlobal = 0;

                // ✅ Kiểm tra kết quả trước khi cập nhật UI
                boolean success = startCollectingRoute();

                if (success) {
                    // ✅ Chỉ cập nhật UI khi thành công
                    fabStart.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
                    fabStart.setText("Dừng thu gom");
                    isCollecting = true;
                }

            } else {
                // ✅ Dừng navigation
                stopNavigationUpdates();
                currentStepGlobal = 0;

                // ✅ XÓA TUYẾN ĐƯỜNG TỐI ƯU
                if (currentRoute != null) {
                    vietmapGL.removeAnnotation(currentRoute);
                    currentRoute = null;
                }

                // ✅ XÓA DỮ LIỆU ROUTE
                polylinePoints.clear();
                routeSteps.clear();

                // ✅ XÓA CACHE (nếu có lưu)
                getSharedPreferences("ROUTE_CACHE", MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply();

                // ✅ Cập nhật UI
                fabStart.setText("Bắt đầu thu gom");
                fabStart.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                isCollecting = false;

                Toast.makeText(this, "Đã dừng thu gom ", Toast.LENGTH_SHORT).show();
            }
        });

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int res = textToSpeech.setLanguage(Locale.forLanguageTag("vi-VN"));
                ttsReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

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
                            moveToCurrentLocation();
                        } else {
                            ActivityCompat.requestPermissions(
                                    this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    LOCATION_PERMISSION_REQUEST_CODE
                            );
                        }

                        loadTasksFromApi();

                        // ✅ Map + Style đã sẵn sàng, gọi thẳng loadSavedRoute
//                        Log.d("ROUTE_LOAD", "Style loaded → loading saved route");
//                        loadSavedRoute();
//                        Log.d("ROUTE_LOAD", "points loaded = " + polylinePoints.size());
                    }
            );
        });

    }

    private void showReportDialog(boolean isBatchReport, Task task) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.layout_dialog_report_issue);

        // Ép dialog rộng gần full màn hình
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );
        }

        EditText edtReason = dialog.findViewById(R.id.edtReason);
        Button btnSubmit = dialog.findViewById(R.id.btnSubmitReason);

        btnSubmit.setOnClickListener(v -> {
            String reason = edtReason.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập lý do!", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();

            if (isBatchReport) {
                reportWholeBatch(reason);
            } else {
                reportSingleBin(task, reason);
            }
        });

        dialog.show();
    }
    private void reportWholeBatch(String reason) {
        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        api.reportBatchIssue(workerId, batchId, reason)
                .enqueue(new Callback<ApiMessage>() {
                    @Override
                    public void onResponse(Call<ApiMessage> call,
                                           Response<ApiMessage> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(TaskDetailActivity.this,
                                    "Đã báo cáo — Manager sẽ xử lý!",
                                    Toast.LENGTH_LONG).show();

                            for (Task t : allTasks) {
                                t.setStatus("ISSUE");
                            }

                            stopNavigationUpdates();
                            redrawMarkers();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiMessage> call, Throwable t) {
                        Toast.makeText(TaskDetailActivity.this,
                                "Lỗi API: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void reportSingleBin(Task task, String reason) {
        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        api.reportTaskIssue(task.getTaskID(), reason)
                .enqueue(new Callback<ApiMessage>() {
                    @Override
                    public void onResponse(Call<ApiMessage> call,
                                           Response<ApiMessage> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(TaskDetailActivity.this,
                                    "Bạn đã báo cáo thùng " + task.getBin().getBinCode(),
                                    Toast.LENGTH_SHORT).show();

                            task.setStatus("ISSUE");
                            updateMarkerForTask(task);

                            // Rebuild route không có thùng ISSUE/COMPLETED
                            List<Task> pending = new ArrayList<>();
                            for (Task t : allTasks) {
                                if (!"ISSUE".equalsIgnoreCase(t.getStatus())
                                        && !"COMPLETED".equalsIgnoreCase(t.getStatus())) {
                                    pending.add(t);
                                }
                            }

                            drawRouteOnly(pending);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiMessage> call, Throwable t) {
                        Toast.makeText(TaskDetailActivity.this,
                                "Lỗi API: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void onBinUpdateReceived(Bin updatedBin) {
        runOnUiThread(() -> {
            Log.d(TAG, "🛰 Received bin update: " + updatedBin.getBinCode() + " | binId=" + updatedBin.getBinId());

            // Tìm task có bin này
            for (Task task : allTasks) {
                if (task.getBin().getBinId() == updatedBin.getBinId()) {
                    // Cập nhật dữ liệu bin trong task
                    task.getBin().setCurrentFill(updatedBin.getCurrentFill());
                    task.getBin().setStatus(updatedBin.getStatus());

                    // Cập nhật marker
                    updateMarkerForTask(task);
                    break;
                }
            }
        });
    }
    private void initIcons() {
        if (iconRed == null) iconRed = getBitmapFromVectorDrawable(R.drawable.ic_bin_red);
        if (iconYellow == null) iconYellow = getBitmapFromVectorDrawable(R.drawable.ic_bin_yellow);
        if (iconGreen == null) iconGreen = getBitmapFromVectorDrawable(R.drawable.ic_bin_green);
        if (iconGrey == null) iconGrey = getBitmapFromVectorDrawable(R.drawable.ic_bin_grey);

        // Fallback khi các icon trên bị null
        if (iconDefault == null) iconDefault = getBitmapFromVectorDrawable(R.drawable.ic_bin_green);
    }

    private Icon getSafeBinIcon(Task task) {
        // Nếu vì lý do nào đó icon chưa init → init lại
        if (iconRed == null || iconYellow == null || iconGreen == null
                || iconGrey == null || iconDefault == null) {
            initIcons();
        }

        Bitmap targetBitmap;

       if (task.getBin() != null && task.getBin().getStatus() == 2) {
            targetBitmap = iconGrey;
        }
        // 3. Chọn theo % đầy
        else {
            int percent = (int) (task.getBin() != null ? task.getBin().getCurrentFill() : 0);

            if (percent >= 80)       targetBitmap = iconRed;
            else if (percent >= 40)  targetBitmap = iconYellow;
            else                     targetBitmap = iconGreen;
        }

        // Fallback cuối cùng
        if (targetBitmap == null) {
            // nếu vẫn null thì dùng iconDefault, nếu iconDefault cũng null
            // thì tạo 1 bitmap 1x1 để tránh app crash
            if (iconDefault != null) {
                targetBitmap = iconDefault;
            } else {
                targetBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                targetBitmap.eraseColor(Color.TRANSPARENT);
            }
        }

        return IconFactory.getInstance(TaskDetailActivity.this)
                .fromBitmap(targetBitmap);
    }


    /**
     * Chuyển Vector Drawable sang Bitmap (GIỐNG HOME)
     */


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
    private void addMarkerForTask(Task task) {
        if (vietmapGL == null) return;

        LatLng pos = new LatLng(task.getBin().getLatitude(), task.getBin().getLongitude());
        int percent = (int) task.getBin().getCurrentFill();

        // ✅ Sử dụng icon mới
        Icon icon = getSafeBinIcon(task);

        String title = task.getBin().getBinCode() + " - " + percent + "% đầy";
        String snippet = "Trạng thái: " + getTaskStatusVietnamese(task.getStatus());

        Marker marker = vietmapGL.addMarker(new MarkerOptions()
                .position(pos)
                .title(title)
                .snippet(snippet)
                .icon(icon));

        // ✅ Lưu vào map
        markerTaskMap.put(marker, task);
        binDataMap.put(marker, task);

        Log.d(TAG, "Added marker for task: " + task.getBin().getBinCode());
    }
    private void updateMarkerForTask(Task task) {
        if (vietmapGL == null) return;

        // Tìm marker cũ
        Marker oldMarker = null;
        for (Map.Entry<Marker, Task> entry : markerTaskMap.entrySet()) {
            if (entry.getValue().getTaskID() == task.getTaskID()) {
                oldMarker = entry.getKey();
                break;
            }
        }

        // Xóa marker cũ
        if (oldMarker != null) {
            vietmapGL.removeMarker(oldMarker);
            markerTaskMap.remove(oldMarker);
            binDataMap.remove(oldMarker);
        }

        // Thêm marker mới
        addMarkerForTask(task);

        Log.d(TAG, "Updated marker for task: " + task.getBin().getBinCode());
    }

    private String getTaskStatusVietnamese(String status) {
        if (status == null) return "Không xác định";

        switch (status.toUpperCase()) {
            case "COMPLETED":
                return "Đã hoàn thành";
            case "DOING":
                return "Đang thực hiện";
            case "OPEN":
                return "Đang chờ xử lý";
            case "CANCELLED":
                return "Đã hủy";
            case "ISSUE":
                return "Có sự cố";
            default:
                return "Không xác định";
        }
    }
    // ✅ HÀM LẤY VỊ TRÍ HIỆN TẠI
    @SuppressWarnings({"MissingPermission"})
    private void moveToCurrentLocation() {
        fusedClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null && vietmapGL != null) {
                        LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15f));
                        Log.d(TAG, "Moved to current location: " + myLatLng);
                    } else {
                        Toast.makeText(this, "Không lấy được vị trí hiện tại", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi lấy vị trí: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                        .useDefaultLocationEngine(true)
                        .locationComponentOptions(customOptions)
                        .build();

        locationComponent.activateLocationComponent(options);
        locationComponent.setLocationComponentEnabled(true);
        locationComponent.setCameraMode(CameraMode.TRACKING);
        locationComponent.setRenderMode(RenderMode.NORMAL);
    }

    private void loadTasksFromApi() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.getTasksInBatch(workerId, batchId).enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, retrofit2.Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allTasks.clear();
                    allTasks.addAll(response.body());

                    // ✅ Khởi tạo icons trước khi vẽ marker
                    initIcons();

                    ExtendedFloatingActionButton fabOptimize = findViewById(R.id.btnOptimize);
                    ExtendedFloatingActionButton fabStart = findViewById(R.id.btnStartCollect);
                    ExtendedFloatingActionButton fabReportBatch = findViewById(R.id.btnReportBatch);

                    boolean allCompleted = true;
                    for (Task t : allTasks) {
                        if (!"COMPLETED".equalsIgnoreCase(t.getStatus())) {
                            allCompleted = false;
                            break;
                        }
                    }

                    if (allCompleted) {
                        fabOptimize.setVisibility(View.GONE);
                        fabStart.setVisibility(View.GONE);
                        fabReportBatch.setVisibility(View.GONE);
                    } else {
                        fabOptimize.setVisibility(View.VISIBLE);
                        fabStart.setVisibility(View.VISIBLE);
                        fabReportBatch.setVisibility(View.VISIBLE);
                    }

                    // ✅ Vẽ marker với icon mới
                    for (Task task : allTasks) {
                        addMarkerForTask(task);
                    }

                    if (!allTasks.isEmpty()) {
                        Task first = allTasks.get(0);
                        vietmapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(first.getBin().getLatitude(), first.getBin().getLongitude()), 15));
                    }

                    vietmapGL.setOnMarkerClickListener(marker -> {
                        Task clickedTask = binDataMap.get(marker);
                        if (clickedTask != null) {
                            showBinDetailBottomSheet(clickedTask);
                        }
                        return true;
                    });
                } else {
                    Toast.makeText(TaskDetailActivity.this, "Không tải được danh sách thùng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                Toast.makeText(TaskDetailActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    public interface RouteCallback {
        void onRouteReady();
    }
    // ====== DRAW ROUTE ONLY ======
    @SuppressWarnings({"MissingPermission"})
    private void drawRouteOnly(List<Task> tasks,RouteCallback callback) {

        //  LỌC TASK CHƯA HOÀN THÀNH
        List<Task> pendingTasks = new ArrayList<>();
        for (Task t : tasks) {
            // Kiểm tra: KHÔNG phải COMPLETED VÀ KHÔNG phải ISSUE
            if (!"COMPLETED".equalsIgnoreCase(t.getStatus()) && !"ISSUE".equalsIgnoreCase(t.getStatus())) {
                pendingTasks.add(t);
            }
        }

        //  NẾU KHÔNG CÓ TASK NÀO ĐỂ TỐI ƯU
        if (pendingTasks.isEmpty()) {
            Toast.makeText(this, "Không có thùng rác đang cần xử lý!", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationComponent lc = vietmapGL.getLocationComponent();
        Location myLocation = (lc != null) ? lc.getLastKnownLocation() : null;

        if (myLocation == null) {
            Toast.makeText(this, "Không lấy được vị trí hiện tại", Toast.LENGTH_SHORT).show();
            return;
        }

        // TẠO URL TỐI ƯU CHỈ CHO pendingTasks
        StringBuilder url = new StringBuilder("https://maps.vietmap.vn/api/route")
                .append("?api-version=1.1")
                .append("&apikey=").append(VMAP_API_KEY)
                .append("&vehicle=car")
                .append("&points_encoded=false")
                .append("&instructions=true");

        //  Điểm bắt đầu = vị trí hiện tại
        url.append("&point=").append(myLocation.getLatitude()).append(",").append(myLocation.getLongitude());

        //  Thêm các thùng rác cần xử lý
        for (Task t : pendingTasks) {
            url.append("&point=").append(t.getBin().getLatitude()).append(",").append(t.getBin().getLongitude());
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url.toString()).build();

        client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(TaskDetailActivity.this,
                                "API lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response)
                    throws IOException {

                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() ->
                            Toast.makeText(TaskDetailActivity.this,
                                    "API trả lỗi: " + response.code(), Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String body = response.body().string();  // Lấy JSON
                try {
                    JSONObject json = new JSONObject(body);
                    JSONArray paths = json.optJSONArray("paths");

                    if (paths == null || paths.length() == 0) {
                        runOnUiThread(() ->
                                Toast.makeText(TaskDetailActivity.this,
                                        "Không tìm thấy tuyến phù hợp", Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }

                    JSONObject path = paths.getJSONObject(0);

                    JSONArray coords = path.getJSONObject("points").getJSONArray("coordinates");
                    polylinePoints.clear();

                    for (int i = 0; i < coords.length(); i++) {
                        JSONArray c = coords.getJSONArray(i);
                        polylinePoints.add(new LatLng(c.getDouble(1), c.getDouble(0)));
                    }

                    routeSteps.clear();
                    JSONArray instructions = path.optJSONArray("instructions");
                    if (instructions != null) {
                        for (int i = 0; i < instructions.length(); i++) {
                            routeSteps.add(instructions.getJSONObject(i));
                        }
                    }

                    runOnUiThread(() -> {
                        if (currentRoute != null) vietmapGL.removeAnnotation(currentRoute);

                        currentRoute = vietmapGL.addPolyline(new PolylineOptions()
                                .addAll(polylinePoints)
                                .color(Color.BLUE)
                                .width(5f));

                        // Lưu lại route nếu cần
                        saveRouteToLocal(polylinePoints, routeSteps);

                        if (!polylinePoints.isEmpty()) {
                            vietmapGL.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(polylinePoints.get(0), 15f)
                            );
                        }
                    });

                } catch (JSONException e) {
                    runOnUiThread(() ->
                            Toast.makeText(TaskDetailActivity.this,
                                    "Parse JSON lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
    private void drawRouteOnly(List<Task> tasks) {
        drawRouteOnly(tasks, null);
    }
    private void saveRouteToLocal(List<LatLng> points, List<JSONObject> steps) {
        try {
            JSONArray pointsArr = new JSONArray();
            for (LatLng p : points) {
                JSONArray item = new JSONArray();
                item.put(p.getLatitude());
                item.put(p.getLongitude());
                pointsArr.put(item);
            }

            JSONArray stepsArr = new JSONArray();
            for (JSONObject s : steps) {
                stepsArr.put(s);
            }

            getSharedPreferences("ROUTE_CACHE", MODE_PRIVATE)
                    .edit()
                    .putString("points", pointsArr.toString())
                    .putString("steps", stepsArr.toString())
                    .apply();

        } catch (Exception e) {
            Log.e("ROUTE_SAVE", "Error: " + e.getMessage());
        }
    }

    private void loadSavedRoute() {
        try {
            SharedPreferences prefs = getSharedPreferences("ROUTE_CACHE", MODE_PRIVATE);
            String pointsStr = prefs.getString("points", null);
            String stepsStr = prefs.getString("steps", null);

            if (pointsStr == null || stepsStr == null) return;

            JSONArray arr = new JSONArray(pointsStr);
            polylinePoints.clear();

            for (int i = 0; i < arr.length(); i++) {
                JSONArray item = arr.getJSONArray(i);
                double lat = item.getDouble(0);
                double lng = item.getDouble(1);
                polylinePoints.add(new LatLng(lat, lng));
            }

            JSONArray insArr = new JSONArray(stepsStr);
            routeSteps.clear();
            for (int i = 0; i < insArr.length(); i++) {
                routeSteps.add(insArr.getJSONObject(i));
            }

            // vẽ polyline ra lại
            runOnUiThread(() -> {
                if (currentRoute != null) vietmapGL.removeAnnotation(currentRoute);
                currentRoute = vietmapGL.addPolyline(new PolylineOptions()
                        .addAll(polylinePoints)
                        .color(Color.BLUE)
                        .width(5f));
            });

        } catch (Exception e) {
            Log.e("ROUTE_LOAD", "Error: " + e.getMessage());
        }
    }


    // ====== START / FOLLOW ROUTE WITH VOICE ======
    private boolean startCollectingRoute() {
        // 1. Kiểm tra Tuyến đường
        if (polylinePoints.isEmpty() || routeSteps.isEmpty()) {
            // Nếu lỗi, hiện Toast và dừng
            Toast.makeText(this, "Chưa có tuyến để theo dõi, hãy bấm Tối ưu trước!", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 2. Kiểm tra TTS
        if (!ttsReady) {
            // Nếu lỗi, hiện Toast và dừng
            Toast.makeText(this, "TTS chưa sẵn sàng để đọc", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 3. Kiểm tra Trạng thái đang điều hướng
        if (isNavigating) {
            // Nếu đã chạy, hiện Toast (nếu cần) và dừng, coi như thành công
            Toast.makeText(this, "Đang điều hướng...", Toast.LENGTH_SHORT).show();
            return true;
        }

        // 4. Bắt đầu điều hướng (Chỉ khi không có lỗi)
        followRouteWithVoice();

        // 5. Cập nhật trạng thái
        isNavigating = true;

        // 6. Báo cáo thành công
        return true;
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
        Button btnReport = view.findViewById(R.id.btnReportThisBin);

        ImageView imgProof = view.findViewById(R.id.imgProof); // ⭐ Thêm dòng này

        // 🗑️ Tiêu đề
        tvTitle.setText("Thùng rác " + task.getBin().getBinCode());

        // ⚙️ Dịch trạng thái sang tiếng Việt
        String statusVi;
        switch (task.getStatus().toUpperCase()) {
            case "COMPLETED":
                statusVi = "Đã hoàn thành";
                break;
            case "OPEN":
                statusVi = "Đang chờ xử lý";
                break;
            case "CANCELLED":
                statusVi = "Đã hủy";
                break;
            case "ISSUE":
                statusVi = "Gặp sự có";
                break;
            default:
                statusVi = "Không xác định";
        }

        tvStatus.setText("Trạng thái: " + statusVi);


        if (task.getStatus().equalsIgnoreCase("COMPLETED")) {
            btnComplete.setVisibility(View.GONE);
            btnReport.setVisibility(View.GONE);
            tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            tvStatus.setText("Đã hoàn thành");

            imgProof.setVisibility(View.VISIBLE);

            // ⭐ ƯU TIÊN HIỂN THỊ ẢNH LOCAL (vừa chụp)
            String afterImage = task.getAfterImage();

            if (afterImage != null && !afterImage.isEmpty()) {
                File localFile = new File(afterImage);

                if (localFile.exists()) {
                    Glide.with(this)
                            .load(localFile)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.placeholder_image)
                            .into(imgProof);
                }
                else if (afterImage.startsWith("http")) {
                    Glide.with(this)
                            .load(afterImage)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.placeholder_image)
                            .into(imgProof);
                }
                // ❌ Không có ảnh hợp lệ
                else {
                    imgProof.setImageResource(R.drawable.placeholder_image);
                }
            } else {
                imgProof.setImageResource(R.drawable.placeholder_image);
            }

        } else {

            imgProof.setVisibility(View.GONE);

            if (task.getStatus().equalsIgnoreCase("OPEN")) {
                btnComplete.setVisibility(View.GONE);
                btnReport.setVisibility(View.GONE);
                tvStatus.setTextColor(Color.parseColor("#FF9800"));
                tvStatus.setText("Đang chờ nhận nhiệm vụ");
            } else if (task.getStatus().equalsIgnoreCase("ISSUE")) {
                btnComplete.setVisibility(View.GONE);
                btnReport.setVisibility(View.GONE);
                tvStatus.setTextColor(Color.parseColor("#FF9800"));
                tvStatus.setText("Đã gặp sự cố");
            }else if (task.getStatus().equalsIgnoreCase("DOING")) {
                btnComplete.setVisibility(View.VISIBLE);
                btnReport.setVisibility(View.VISIBLE);
                tvStatus.setTextColor(Color.parseColor("#FF9800"));
                tvStatus.setText("Đang chờ xử lý");
            }
        }


        btnComplete.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(TaskDetailActivity.this, CompleteTaskActivity.class);
            intent.putExtra("taskId", task.getTaskID());
            intent.putExtra("binCode", task.getBin().getBinCode());
            intent.putExtra("binLat", task.getBin().getLatitude());
            intent.putExtra("binLng", task.getBin().getLongitude());
            intent.putExtra("currentFill", task.getBin().getCurrentFill());
            intent.putExtra("capacity", task.getBin().getCapacity());
            intent.putExtra("bin_adrress", task.getBin().getStreet() + "," +
                    task.getBin().getProvinceName() + "," +
                    task.getBin().getProvinceName());

            completeTaskLauncher.launch(intent);
        });
        Button btnReportThisBin = view.findViewById(R.id.btnReportThisBin);

        btnReportThisBin.setOnClickListener(v -> {
            dialog.dismiss();
            showSingleBinIssueDialog(task);
        });
        dialog.show();
    }

    private void showSingleBinIssueDialog(Task task) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.layout_dialog_report_issue);

        // Full width cho dialog
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        EditText edtReason = dialog.findViewById(R.id.edtReason);
        Button btnSubmit = dialog.findViewById(R.id.btnSubmitReason);

        // Đặt title động

        btnSubmit.setOnClickListener(v -> {
            String reason = edtReason.getText().toString().trim();

            if (reason.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập lý do!", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            reportSingleBin(task, reason); // Gửi lý do vào API
        });

        dialog.show();
    }
    private final ActivityResultLauncher<Intent> completeTaskLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String status = result.getData().getStringExtra("status");
                    int taskId = result.getData().getIntExtra("taskId", -1);
                    String proofImagePath = result.getData().getStringExtra("proofImagePath");

                    if ("COMPLETED".equals(status)) {

                        for (Task t : allTasks) {
                            if (t.getTaskID() == taskId) {
                                t.setStatus("COMPLETED");
                                if (proofImagePath != null) {
                                    t.setAfterImage(proofImagePath); // Lưu local path tạm thời
                                }
                                break;
                            }
                        }

                        List<Task> pending = new ArrayList<>();
                        for (Task t : allTasks) {
                            if (!"COMPLETED".equalsIgnoreCase(t.getStatus())) {
                                pending.add(t);
                            }
                        }

                        polylinePoints.clear();
                        routeSteps.clear();
                        if (currentRoute != null) vietmapGL.removeAnnotation(currentRoute);
                        getSharedPreferences("ROUTE_CACHE", MODE_PRIVATE).edit().clear().apply();

                        redrawMarkers();

                        if (!pending.isEmpty()) {

                            drawRouteOnly(pending, () -> {
                                // ⭐ Đảm bảo rằng callback này chạy trên UI Thread (thường là mặc định trong Android)

                                currentStepGlobal = 0;

                                // 1. Gọi hàm và lấy kết quả thành công/thất bại
                                boolean routeStartedSuccessfully = startCollectingRoute();

                                // 2. CHỈ CẬP NHẬT UI KHI HÀM BÁO THÀNH CÔNG (true)
                                if (routeStartedSuccessfully) {
                                    ExtendedFloatingActionButton fabStart = findViewById(R.id.btnStartCollect);
                                    fabStart.setText("Dừng thu gom");
                                    fabStart.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
                                    isCollecting = true;

                                    Toast.makeText(this, "Bắt đầu thu gom!", Toast.LENGTH_SHORT).show();
                                }
                            });

                        } else {
                            Toast.makeText(this, "Tất cả thùng đã hoàn thành!", Toast.LENGTH_SHORT).show();
                        }


                    }
                }
            });

private void redrawMarkers() {
    // Xóa tất cả marker nhưng giữ polyline
    for (Marker m : markerTaskMap.keySet()) {
        vietmapGL.removeAnnotation(m);
    }
    markerTaskMap.clear();
    binDataMap.clear();

    // Vẽ lại marker với icon mới
    for (Task task : allTasks) {
        addMarkerForTask(task);
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


//    private int getStatusIcon(Task task) {
//        if (task == null) return R.drawable.ic_bin_red;
//
//        String status = task.getStatus() != null ? task.getStatus().toUpperCase() : "";
//        double fill = task.getBin().getCurrentFill(); // giả sử model Bin có currentFill (%)
//
//        if ("COMPLETED".equals(status)) {
//            return R.drawable.ic_bin_green;
//        }
//
//        if (fill >= 80) {
//            return R.drawable.ic_bin_red;
//        }
//
//        // 🟡 Đang xử lý
//        if (fill >= 40) {
//            return R.drawable.ic_bin_yellow;
//        }
//
//
//        // Mặc định (nếu có status lạ)
//        return R.drawable.ic_bin_red;
//
//    }

//        private Bitmap getBitmapFromVectorDrawable(int drawableId) {
//        Drawable drawable = ContextCompat.getDrawable(this, drawableId);
//        if (drawable == null) {
//            Bitmap empty = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
//            empty.eraseColor(Color.TRANSPARENT);
//            return empty;
//        }
//        Bitmap bitmap = Bitmap.createBitmap(
//                Math.max(1, drawable.getIntrinsicWidth()),
//                Math.max(1, drawable.getIntrinsicHeight()),
//                Bitmap.Config.ARGB_8888
//        );
//        Canvas canvas = new Canvas(bitmap);
//        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
//        drawable.draw(canvas);
//        return bitmap;
//    }



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
        super.onDestroy(); // Cần thêm super.onDestroy() để hoàn chỉnh
    }

    // Thêm onSaveInstanceState và onLowMemory để hoàn chỉnh MapView
    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
    @Override public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

}