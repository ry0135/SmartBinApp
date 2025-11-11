package com.example.smartbinapp.adapter;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smartbinapp.R;
import com.example.smartbinapp.ReportsListActivity;
import com.example.smartbinapp.TaskSummaryActivity;
import com.example.smartbinapp.model.Notification;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private final List<Object> mixedList = new ArrayList<>();
    private final Context context;

    public NotificationAdapter(Context context, List<Notification> notifications) {
        this.context = context;

        // ✅ Sắp xếp thông báo mới nhất lên đầu
        Collections.sort(notifications, (a, b) -> {
            long t1 = parseTime(b.getCreatedAt());
            long t2 = parseTime(a.getCreatedAt());
            return Long.compare(t1, t2);
        });

        groupNotifications(notifications);
    }

    // 🔹 Nhóm thông báo theo ngày
    private void groupNotifications(List<Notification> list) {
        List<Notification> today = new ArrayList<>();
        List<Notification> week = new ArrayList<>();
        List<Notification> older = new ArrayList<>();

        long now = System.currentTimeMillis();

        for (Notification n : list) {
            long created = parseTime(n.getCreatedAt());
            long diff = now - created;
            long days = diff / (1000 * 60 * 60 * 24);

            if (days < 1) today.add(n);
            else if (days < 7) week.add(n);
            else older.add(n);
        }

        if (!today.isEmpty()) {
            mixedList.add("Hôm nay");
            mixedList.addAll(today);
        }
        if (!week.isEmpty()) {
            mixedList.add("Tuần này");
            mixedList.addAll(week);
        }
        if (!older.isEmpty()) {
            mixedList.add("Cũ hơn");
            mixedList.addAll(older);
        }
    }

    private static long parseTime(String createdAt) {
        try {
            if (createdAt == null) return 0;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            return sdf.parse(createdAt).getTime();
        } catch (Exception e) {
            Log.e("NotificationAdapter", "Parse time error: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mixedList.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return mixedList.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_header_section, parent, false);
            return new HeaderHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new ItemHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).tvHeader.setText((String) mixedList.get(position));
        } else {
            Notification n = (Notification) mixedList.get(position);
            ItemHolder vh = (ItemHolder) holder;

            vh.tvTitle.setText(n.getTitle());
            vh.tvMessage.setText(n.getMessage());

            // ✅ Format ngắn gọn “dd/MM/yyyy HH:mm”
            String dateText = formatDate(n.getCreatedAt());
            vh.tvTime.setText(dateText);

            // 🔵 Nếu chưa đọc -> sáng rõ, có chấm xanh
            // ⚪ Nếu đã đọc -> làm mờ, ẩn chấm
            if (n.isRead()) {
                vh.viewDot.setVisibility(View.GONE);
                vh.itemView.setAlpha(0.5f); // làm mờ
            } else {
                vh.viewDot.setVisibility(View.VISIBLE);
                vh.itemView.setAlpha(1f); // sáng rõ
            }

            Glide.with(vh.itemView.getContext())
                    .load(n.getImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(vh.imgThumb);

            // Khi click -> đánh dấu đã đọc + mở trang phù hợp
            vh.itemView.setOnClickListener(v -> {
                if (!n.isRead()) {
                    n.setRead(true);
                    vh.viewDot.setVisibility(View.GONE);
                    animateFade(vh.itemView); // 👈 hiệu ứng mờ dần
                    markAsRead(n.getNotificationID());
                }

                Intent intent = null;
                String type = n.getType() != null ? n.getType().toUpperCase() : "";

                switch (type) {
                    case "TASK":
                    case "TASK_COMPLETE":
                        intent = new Intent(context, TaskSummaryActivity.class);
                        break;
                    case "REPORT":
                        intent = new Intent(context, ReportsListActivity.class);
                        break;
                    default:
                        break;
                }

                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            });
        }
    }

    // 🔹 Format ngày
    private String formatDate(String createdAt) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Date date = in.parse(createdAt);
            SimpleDateFormat out = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            return out.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    // 🔹 Hiệu ứng mờ dần khi đọc
    private void animateFade(View view) {
        ObjectAnimator fade = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.5f);
        fade.setDuration(400);
        fade.setInterpolator(new android.view.animation.DecelerateInterpolator());
        fade.start();
    }

    // 🔹 Gọi API đánh dấu đã đọc
    private void markAsRead(int notificationId) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.markNotificationAsRead(notificationId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("NotificationAdapter", "✅ Đã đánh dấu đọc: ID=" + notificationId);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("NotificationAdapter", "❌ Lỗi khi mark read: " + t.getMessage());
            }
        });
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;
        HeaderHolder(View v) {
            super(v);
            tvHeader = v.findViewById(R.id.tv_header);
        }
    }

    static class ItemHolder extends RecyclerView.ViewHolder {
        View viewDot;
        ImageView imgThumb;
        TextView tvTitle, tvMessage, tvTime;
        ItemHolder(View v) {
            super(v);
            viewDot = v.findViewById(R.id.view_unread_dot);
            imgThumb = v.findViewById(R.id.img_thumb);
            tvTitle = v.findViewById(R.id.tv_title);
            tvMessage = v.findViewById(R.id.tv_message);
            tvTime = v.findViewById(R.id.tv_time);
        }
    }
}
