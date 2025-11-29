package com.example.smartbinapp.service;

import android.annotation.SuppressLint;
import android.os.Handler;
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

    private static final String TAG = "BinWebSocket";
    private static final String WS_URL = "wss://smartbin-vn.duckdns.org/ws-bin";

    private StompClient stompClient;
    private Disposable topicSubscription;
    private BinUpdateListener listener;

    private boolean isConnected = false;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Date.class,
                    (JsonDeserializer<Date>) (json, typeOfT, context) ->
                            new Date(json.getAsJsonPrimitive().getAsLong()))
            .create();

    public void setListener(BinUpdateListener listener) {
        this.listener = listener;
    }


    // =========================================================
    //                   CONNECT WEBSOCKET
    // =========================================================
    @SuppressLint("CheckResult")
    public void connect() {

        if (isConnected) {
            Log.w(TAG, "⚠️ WebSocket already connected → skip");
            return;
        }

        Log.d(TAG, "🔌 Connecting WebSocket...");
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_URL);

        List<StompHeader> headers = new ArrayList<>();
        headers.add(new StompHeader("accept-version", "1.1,1.2"));
        headers.add(new StompHeader("heart-beat", "10000,10000"));

        stompClient.connect(headers);

        // ---------------- Lifecycle ----------------
        stompClient.lifecycle().subscribe(event -> {
            switch (event.getType()) {

                case OPENED:
                    Log.i(TAG, "🔥 STOMP CONNECTED");
                    isConnected = true;
                    subscribeToTopic(); // subscribe ngay sau khi connect
                    break;

                case ERROR:
                    Log.e(TAG, "❌ STOMP ERROR", event.getException());
                    isConnected = false;
                    break;

                case CLOSED:
                    Log.w(TAG, "⚠️ STOMP CLOSED");
                    isConnected = false;
                    break;
            }
        });
    }


    // =========================================================
    //               🟢 SUBSCRIBE SAFE WITH ERROR HANDLER
    // =========================================================
    @SuppressLint("CheckResult")
    private void subscribeToTopic() {

        if (!isConnected) {
            Log.w(TAG, "⛔ subscribeToTopic() called before connected → skip");
            return;
        }

        Log.d(TAG, "🔔 Subscribing /topic/binUpdates");

        topicSubscription = stompClient.topic("/topic/binUpdates")
                .subscribe(
                        msg -> { // onNext
                            String payload = msg.getPayload();
                            Log.d(TAG, "♻️ Received: " + payload);

                            try {
                                Bin updated = gson.fromJson(payload, Bin.class);
                                if (listener != null) listener.onBinUpdated(updated);
                            } catch (Exception e) {
                                Log.e(TAG, "❗ JSON parse failed: " + e.getMessage());
                            }
                        },
                        error -> { // onError → KHÔNG CRASH NỮA
                            Log.e(TAG, "❌ Subscribe ERROR", error);
                        }
                );
    }


    // =========================================================
    //                      DISCONNECT SAFE
    // =========================================================
//    public void disconnect() {
//
//        Log.w(TAG, "🔌 disconnect() called");
//
//        try {
//
//            if (topicSubscription != null && !topicSubscription.isDisposed()) {
//                topicSubscription.dispose();
//            }
//
//            if (stompClient != null && isConnected) {
//                stompClient.disconnect()
//                        .subscribe(
//                                () -> Log.d(TAG, "🧹 STOMP disconnected"),
//                                error -> Log.e(TAG, "❌ Disconnect ERROR", error)
//                        );
//            }
//
//        } catch (Exception e) {
//            Log.e(TAG, "❌ disconnect() exception", e);
//        } finally {
//            isConnected = false;
//        }
//    }
    public void disconnect() {
        if (topicSubscription != null) topicSubscription.dispose();
        if (stompClient != null) stompClient.disconnect();
    }
}
