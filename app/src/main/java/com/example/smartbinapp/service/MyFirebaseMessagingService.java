package com.example.smartbinapp.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.smartbinapp.R;
import com.example.smartbinapp.TaskDetailActivity;
import com.example.smartbinapp.TaskSummaryActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "task_channel";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM", "✅ New token: " + token);

        // TODO: Gửi token này về server backend (gắn với workerId)
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        createNotificationChannel();

        String title = "Nhiệm vụ mới";
        String body = "Bạn có nhiệm vụ mới được giao";

        Intent intent = null;

        // Nếu có notification payload
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        // Nếu có data payload
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            Log.d("FCM", "📦 Data: " + data);

            if (data.containsKey("title")) title = data.get("title");
            if (data.containsKey("body")) body = data.get("body");

            // Xử lý mở activity theo batchId hoặc taskId
            if (data.containsKey("batchId")) {
                String batchId = data.get("batchId");
                intent = new Intent(this, TaskDetailActivity.class);
                intent.putExtra("batchId", batchId);
            }
//            else if (data.containsKey("taskId")) {
//                String taskId = data.get("taskId");
//                intent = new Intent(this, TaskDetailActivity.class);
//                intent.putExtra("taskId", taskId);
//            }
        }

        if (intent == null) {
            // fallback: mở TaskSummaryActivity mặc định
            intent = new Intent(this, TaskSummaryActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Tạo thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // icon bắt buộc
                .setContentTitle(title != null ? title : "Thông báo")
                .setContentText(body != null ? body : "")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent); // gắn intent khi click

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);

        // Check quyền Android 13+
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
                || Build.VERSION.SDK_INT < 33) {
            int notifyId = (int) System.currentTimeMillis();
            manager.notify(notifyId, builder.build());
        } else {
            Log.e("FCM", "❌ Chưa có quyền POST_NOTIFICATIONS");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "Task Notifications";
            String description = "Thông báo khi có nhiệm vụ mới";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
