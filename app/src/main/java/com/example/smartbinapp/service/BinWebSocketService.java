package com.example.smartbinapp.service;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.smartbinapp.listener.BinUpdateListener;
import com.example.smartbinapp.model.Bin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.Disposable;
import okhttp3.OkHttpClient;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;

public class BinWebSocketService {

    private static final String TAG = "BinWebSocket";
    private static final String WS_URL = "wss://smartbinx.duckdns.org/ws-bin";

    // ✅ Reconnect configuration
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long INITIAL_RECONNECT_DELAY = 2000; // 2 seconds

    private static BinWebSocketService instance;

    public static synchronized BinWebSocketService getInstance() {
        if (instance == null) instance = new BinWebSocketService();
        return instance;
    }

    private StompClient stompClient;
    private Disposable topicSubscription;
    private Disposable lifecycleDisposable; // ✅ Thêm để dispose lifecycle
    private BinUpdateListener listener;

    private boolean isConnected = false;
    private boolean isConnecting = false; // ✅ Tránh kết nối trùng lặp
    private int reconnectAttempts = 0;

    // ✅ Handler để xử lý reconnect
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Date.class,
                    (JsonDeserializer<Date>) (json, typeOfT, context) ->
                            new Date(json.getAsJsonPrimitive().getAsLong()))
            .create();

    // =========================================================
    //   🔗 SET LISTENER
    // =========================================================
    public void setListener(BinUpdateListener listener) {
        this.listener = listener;
    }

    // =========================================================
    //   🔌 CONNECT WITH AUTO-RECONNECT
    // =========================================================
    @SuppressLint("CheckResult")
    public void connect() {
        if (isConnected) {
            Log.d(TAG, "⚠ Already connected → skip");
            return;
        }

        if (isConnecting) {
            Log.d(TAG, "⚠ Connection in progress → skip");
            return;
        }

        isConnecting = true;
        Log.d(TAG, "🔌 Connecting STOMP WebSocket... (Attempt: " + (reconnectAttempts + 1) + ")");

        // ✅ Cấu hình OkHttpClient với timeout & ping
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .pingInterval(20, TimeUnit.SECONDS) // ⭐ Giữ kết nối sống
                .retryOnConnectionFailure(true)
                .build();

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_URL, null, client);

        List<StompHeader> headers = new ArrayList<>();
        headers.add(new StompHeader("accept-version", "1.1,1.2"));
        headers.add(new StompHeader("heart-beat", "10000,10000"));

        // ✅ Lắng nghe lifecycle events
        lifecycleDisposable = stompClient.lifecycle().subscribe(event -> {
            switch (event.getType()) {

                case OPENED:
                    Log.i(TAG, "🔥 STOMP CONNECTED");
                    isConnected = true;
                    isConnecting = false;
                    reconnectAttempts = 0; // ✅ Reset khi kết nối thành công
                    subscribeToTopic();
                    break;

                case ERROR:
                    Log.e(TAG, "❌ STOMP ERROR", event.getException());
                    isConnected = false;
                    isConnecting = false;
                    scheduleReconnect(); // ✅ Tự động reconnect
                    break;

                case CLOSED:
                    Log.w(TAG, "⚠ WebSocket CLOSED");
                    isConnected = false;
                    isConnecting = false;
                    scheduleReconnect(); // ✅ Tự động reconnect
                    break;
            }
        }, throwable -> {
            Log.e(TAG, "❌ Lifecycle subscription error", throwable);
            isConnected = false;
            isConnecting = false;
            scheduleReconnect();
        });

        stompClient.connect(headers);
    }

    // =========================================================
    //   🔄 AUTO RECONNECT WITH EXPONENTIAL BACKOFF
    // =========================================================
    private void scheduleReconnect() {
        // ✅ Hủy tất cả pending reconnect tasks
        reconnectHandler.removeCallbacksAndMessages(null);

        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "❌ Max reconnect attempts reached (" + MAX_RECONNECT_ATTEMPTS + "). Stopping.");
            reconnectAttempts = 0; // Reset để có thể thử lại sau
            return;
        }

        reconnectAttempts++;

        // ✅ Exponential backoff: 2s, 4s, 8s, 16s, 32s
        long delayMillis = INITIAL_RECONNECT_DELAY * (long) Math.pow(2, reconnectAttempts - 1);

        Log.d(TAG, "🔄 Scheduling reconnect in " + (delayMillis / 1000) + "s... (Attempt " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS + ")");

        reconnectHandler.postDelayed(() -> {
            Log.d(TAG, "🔄 Attempting to reconnect...");
            disconnect(); // Đảm bảo ngắt kết nối cũ
            connect();
        }, delayMillis);
    }

    // =========================================================
    //   🟢 SUBSCRIBE SAFE
    // =========================================================
    @SuppressLint("CheckResult")
    private void subscribeToTopic() {
        if (!isConnected) {
            Log.w(TAG, "⛔ subscribeToTopic() called before connected");
            return;
        }

        Log.d(TAG, "🔔 Subscribing: /topic/binUpdates");

        topicSubscription = stompClient.topic("/topic/binUpdates").subscribe(
                msg -> {
                    String payload = msg.getPayload();
                    Log.d(TAG, "📥 Received: " + payload);

                    try {
                        Bin updated = gson.fromJson(payload, Bin.class);
                        if (listener != null) {
                            listener.onBinUpdated(updated);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❗ JSON parse error: " + e.getMessage(), e);
                    }
                },
                error -> {
                    Log.e(TAG, "❌ Subscribe error", error);
                    // ✅ Nếu subscription lỗi, thử reconnect
                    scheduleReconnect();
                }
        );
    }

    // =========================================================
    //   📤 SEND MESSAGE (Safe)
    // =========================================================
    @SuppressLint("CheckResult")
    public void send(String destination, String jsonBody) {
        if (!isConnected) {
            Log.e(TAG, "⛔ Cannot send → WebSocket NOT connected");
            return;
        }

        stompClient.send(destination, jsonBody)
                .subscribe(
                        () -> Log.d(TAG, "📤 Sent → " + destination),
                        error -> Log.e(TAG, "❌ Send failed: " + error.getMessage())
                );
    }

    public void sendWebSocketNotification(int reportId, String type, String binCode) {
        try {
            JSONObject json = new JSONObject();
            json.put("reportId", reportId);
            json.put("type", type);
            json.put("binCode", binCode);

            // ✅ Đảm bảo đã connect trước khi gửi
            if (!isConnected) {
                connect();
                // ⚠️ Có thể cần delay để chờ kết nối xong
                reconnectHandler.postDelayed(() -> {
                    if (isConnected) {
                        send("/app/report/new", json.toString());
                    } else {
                        Log.e(TAG, "❌ Cannot send notification: not connected after retry");
                    }
                }, 2000);
            } else {
                send("/app/report/new", json.toString());
            }

        } catch (Exception e) {
            Log.e(TAG, "Send notification error: " + e.getMessage(), e);
        }
    }

    // =========================================================
    //   🔌 DISCONNECT
    // =========================================================
    public void disconnect() {
        Log.d(TAG, "🔌 Disconnecting...");

        // ✅ Hủy tất cả pending reconnect
        reconnectHandler.removeCallbacksAndMessages(null);

        if (topicSubscription != null && !topicSubscription.isDisposed()) {
            topicSubscription.dispose();
            topicSubscription = null;
        }

        if (lifecycleDisposable != null && !lifecycleDisposable.isDisposed()) {
            lifecycleDisposable.dispose();
            lifecycleDisposable = null;
        }

        if (stompClient != null) {
            stompClient.disconnect();
            stompClient = null;
        }

        isConnected = false;
        isConnecting = false;
        reconnectAttempts = 0;

        Log.d(TAG, "✅ Disconnected");
    }

    // =========================================================
    //   📊 STATUS CHECK
    // =========================================================
    public boolean isConnected() {
        return isConnected;
    }

    public void forceReconnect() {
        Log.d(TAG, "🔄 Force reconnecting...");
        disconnect();
        reconnectAttempts = 0;
        connect();
    }
}