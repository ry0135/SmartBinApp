package com.example.smartbinapp.service;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.gson.Gson;
import com.example.smartbinapp.model.Task;
import com.example.smartbinapp.listener.TaskUpdateListener;

import io.reactivex.disposables.Disposable;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

public class TaskWebSocketService {

    private static final String TAG = "TaskWebSocket";
    private static final String WS_URL = "ws://13.250.55.46:8080/SmartBinWeb/ws-bin"; //
    private final Gson gson = new Gson();

    private StompClient stompClient;
    private Disposable topicSubscription;
    private TaskUpdateListener listener;

    public void setListener(TaskUpdateListener listener) {
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
                    subscribeToTaskUpdates();
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

    private void subscribeToTaskUpdates() {
        topicSubscription = stompClient.topic("/topic/task-updates").subscribe(topicMessage -> {
            String payload = topicMessage.getPayload();
            Log.d(TAG, "📩 Received: " + payload);

            try {
                Task updatedTask = gson.fromJson(payload, Task.class);
                Log.d(TAG, "🧩 TaskID: " + updatedTask.getTaskID() +
                        " | Status: " + updatedTask.getStatus());

                if (listener != null) {
                    listener.onTaskUpdated(updatedTask); // ✅ Báo về UI
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
