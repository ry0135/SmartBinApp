package com.example.smartbinapp.service;

import android.annotation.SuppressLint;
import android.util.Log;

import com.example.smartbinapp.listener.BinUpdateListener;
import com.example.smartbinapp.model.Bin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import java.util.Date;

import io.reactivex.disposables.Disposable;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

public class BinWebSocketService {

    private static final String TAG = "BinWebSocket";
//    private static final String WS_URL = "ws://10.0.2.2:8080/SmartBinWeb_war/ws-bin/websocket";
    private static final String WS_URL = "ws://54.254.193.147:8080/SmartBinWeb/ws-bin";

    private StompClient stompClient;
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) ->
                    new Date(json.getAsJsonPrimitive().getAsLong()))
            .create();
    private Disposable topicSubscription;
    private BinUpdateListener listener;

    public void setListener(BinUpdateListener listener) {
        this.listener = listener;
    }

    @SuppressLint("CheckResult")
    public void connect() {
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_URL);
        stompClient.connect();

        stompClient.lifecycle().subscribe(lifecycleEvent -> {
            switch (lifecycleEvent.getType()) {
                case OPENED:
                    Log.i(TAG, "✅ WebSocket connected");
                    subscribeToBinUpdates();
                    break;
                case ERROR:
                    Log.e(TAG, "❌ WebSocket error", lifecycleEvent.getException());
                    break;
                case CLOSED:
                    Log.w(TAG, "⚠️ WebSocket closed");
                    break;
            }
        });
    }

    private void subscribeToBinUpdates() {
        topicSubscription = stompClient.topic("/topic/binUpdates").subscribe(topicMessage -> {
            String payload = topicMessage.getPayload();
            Log.d(TAG, "♻️ Received: " + payload);
            try {
                Bin updatedBin = gson.fromJson(payload, Bin.class);
                Log.d(TAG, "🔹 BinID: " + updatedBin.getBinId()
                        + " - Fill: " + updatedBin.getCurrentFill()
                        + " - Status: " + updatedBin.getStatus());

                if (listener != null) {
                    listener.onBinUpdated(updatedBin); // ✅ Báo về UI
                }

            } catch (Exception e) {
                Log.e(TAG, "❗ JSON parse error: " + e.getMessage());
            }
        });
    }

    public void disconnect() {
        if (topicSubscription != null) topicSubscription.dispose();
        if (stompClient != null) stompClient.disconnect();
    }
}
