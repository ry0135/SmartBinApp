package com.example.smartbinapp.service;

import android.annotation.SuppressLint;
import android.util.Log;

import com.example.smartbinapp.listener.BinUpdateListener;
import com.example.smartbinapp.model.Bin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.disposables.Disposable;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;

public class BinWebSocketService {
    private boolean isConnected = false;
    private static final String TAG = "BinWebSocket";

    // CHÍNH XÁC
    private static final String WS_URL = "wss://smartbin-vn.duckdns.org/ws-bin";

    private StompClient stompClient;
    private Disposable topicSubscription;
    private BinUpdateListener listener;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) ->
                    new Date(json.getAsJsonPrimitive().getAsLong()))
            .create();

    public void setListener(BinUpdateListener listener) {
        this.listener = listener;
    }

    @SuppressLint("CheckResult")
    public void connect() {
        if (isConnected) return;

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_URL);

        List<StompHeader> headers = new ArrayList<>();
        headers.add(new StompHeader("accept-version", "1.1,1.2"));
        headers.add(new StompHeader("heart-beat", "10000,10000"));

        // ✅ CHỈ CONNECT 1 LẦN
        stompClient.connect(headers);

        stompClient.lifecycle().subscribe(lifecycleEvent -> {
            switch (lifecycleEvent.getType()) {

                case OPENED:
                    isConnected = true;   // 🔥 set flag
                    Log.i(TAG, "🔥 STOMP OPENED - WebSocket CONNECTED");
                    break;

                case ERROR:
                    Log.e(TAG, "❌ WebSocket error", lifecycleEvent.getException());
                    break;

                case CLOSED:
                    isConnected = false;  // 🔥 reset flag
                    Log.w(TAG, "⚠️ WebSocket CLOSED");
                    break;
            }
        });

        subscribeSafely();
    }

    // CHỐNG SUBSCRIBE quá sớm
    private void subscribeSafely() {
        new android.os.Handler().postDelayed(() -> {
            Log.d(TAG, "🔔 Subscribing to /topic/binUpdates...");

            topicSubscription = stompClient.topic("/topic/binUpdates")
                    .subscribe(message -> {
                        String payload = message.getPayload();
                        Log.d(TAG, "♻️ Received: " + payload);

                        try {
                            Bin updated = gson.fromJson(payload, Bin.class);
                            if (listener != null) listener.onBinUpdated(updated);
                        } catch (Exception e) {
                            Log.e(TAG, "❗ JSON parse failed: " + e.getMessage());
                        }
                    });

        }, 800); // Delay 800ms
    }

    public void disconnect() {
        try {
            if (topicSubscription != null) topicSubscription.dispose();
            if (stompClient != null) stompClient.disconnect();
        } finally {
            isConnected = false;  // 🔥 reset
        }
    }
}
