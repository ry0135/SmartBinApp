package com.example.smartbinapp.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbinapp.R;
import com.example.smartbinapp.model.TaskSummary;
import com.example.smartbinapp.network.ApiService;
import com.example.smartbinapp.network.RetrofitClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskSummaryAdapter extends RecyclerView.Adapter<TaskSummaryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(TaskSummary summary);
    }

    private List<TaskSummary> list;
    private OnItemClickListener listener;
    private Context context;
    private ApiService apiService;

    public TaskSummaryAdapter(List<TaskSummary> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_summary, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TaskSummary item = list.get(position);

        // Ghi chú
        holder.tvNote.setText(item.getNote() != null ? item.getNote() : "Không có ghi chú");

        // Ưu tiên
        holder.tvPriority.setText("Độ ưu tiên: " + item.getMinPriority());


        String raw = item.getCreatedAt();

        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = input.parse(raw);

            // Định dạng riêng ngày - giờ
            SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat formatTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

            holder.tvDate.setText(formatDate.format(date));
            holder.tvTime.setText(formatTime.format(date));

        } catch (Exception e) {
            holder.tvDate.setText(raw);
            holder.tvTime.setText(""); // fallback
        }
        // Trạng thái
        String status = item.getStatus();
        String statusVi;
        int color;

        if (status == null || status.trim().isEmpty()) {
            statusVi = "Không xác định";
            color = Color.GRAY;
        } else {
            String normalized = status.trim().toUpperCase();
            Log.d("TaskStatus", "Status from API: '" + normalized + "'");
            switch (normalized) {
                case "COMPLETED":
                    statusVi = "Đã hoàn thành";
                    holder.tvStatus.setBackgroundResource(R.drawable.status_chip_green);
                    break;

                case "DOING":
                    statusVi = "Đang thực hiện";
                    holder.tvStatus.setBackgroundResource(R.drawable.status_chip_purple);
                    break;

                case "OPEN":
                    statusVi = "Đang mở";
                    holder.tvStatus.setBackgroundResource(R.drawable.status_chip_yellow);
                    break;
                case "CANCELLED":
                    statusVi = "Đã hủy";
                    holder.tvStatus.setBackgroundResource(R.drawable.status_chip_red);
                    break;

                case "ISSUE":
                    statusVi = "Gặp sự cố";
                    holder.tvStatus.setBackgroundResource(R.drawable.status_chip_red);
                    break;

                default:
                    statusVi = "Không xác định";
                    holder.tvStatus.setBackgroundResource(R.drawable.status_chip_yellow);
                    break;
            }
        }

        holder.tvStatus.setText(statusVi);
        holder.tvStatus.setTextColor(Color.WHITE);
        holder.tvStatus.setPadding(16, 8, 16, 8);
        holder.tvStatus.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        // Nút hành động
        if ("OPEN".equalsIgnoreCase(status)) {
            holder.btnAction.setVisibility(View.VISIBLE);
            holder.btnAction.setText("Nhận nhiệm vụ");
            holder.btnAction.setBackgroundColor(Color.parseColor("#4CAF50"));

            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setText("Hủy nhiệm vụ");
            holder.btnCancel.setBackgroundColor(Color.parseColor("#ff0033"));


        } else {
            holder.btnAction.setVisibility(View.GONE);
            holder.btnCancel.setVisibility(View.GONE);
        }

        // Sự kiện click nút
        holder.btnAction.setOnClickListener(v -> {
            if ("OPEN".equalsIgnoreCase(status)) {
                showConfirmDialog(item, "DOING", "Nhận nhiệm vụ này?");
            }
        });

        holder.btnCancel.setOnClickListener(v -> {
            if ("OPEN".equalsIgnoreCase(status)) {
                showConfirmDialog(item, "CANCELLED", "Hủy nhiệm vụ này?");
            }
        });

        // Sự kiện click item mở chi tiết
        holder.itemView.setOnClickListener(v -> {
            if ("CANCELLED".equalsIgnoreCase(item.getStatus())) {
                Toast.makeText(v.getContext(), "❌ Nhiệm vụ đã bị hủy – không thể xem chi tiết", Toast.LENGTH_SHORT).show();
                return; // ⛔ KHÔNG CHO MỞ DETAIL
            }

            listener.onItemClick(item); // ⬅️ Chỉ mở nếu không bị hủy
        });

    }

    // 📅 Hàm định dạng ngày ISO → dd/MM/yyyy
    private String formatDate(String inputDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = inputFormat.parse(inputDate);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return inputDate;
        }
    }

    private String formatDateNow() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }
    private void showConfirmDialog(TaskSummary task, String newStatus, String message) {
        new AlertDialog.Builder(context)
                .setTitle("Xác nhận")
                .setMessage(message)
                .setPositiveButton("Đồng ý", (dialog, which) -> updateTaskStatus(task, newStatus))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateTaskStatus(TaskSummary task, String newStatus) {
        Call<Void> call = apiService.updateTaskStatus(task.getBatchId(), newStatus);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "✅ Cập nhật trạng thái thành công!", Toast.LENGTH_SHORT).show();
                    task.setStatus(newStatus);
                    notifyDataSetChanged();
                } else {
                    Toast.makeText(context, "❌ Lỗi khi cập nhật trạng thái!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(context, "⚠️ Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNote, tvPriority, tvStatus, tvDate, tvTime; // 🆕 thêm tvDate
        Button btnAction, btnCancel;

        ViewHolder(View v) {
            super(v);
            tvNote = v.findViewById(R.id.tvNote);
            tvPriority = v.findViewById(R.id.tvPriority);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvDate = v.findViewById(R.id.tvDate); // 🆕 ánh xạ TextView ngày
            tvTime = v.findViewById(R.id.tvTime);
            btnAction = v.findViewById(R.id.btnAction);
            btnCancel = v.findViewById(R.id.btnCancel);

        }
    }
}
