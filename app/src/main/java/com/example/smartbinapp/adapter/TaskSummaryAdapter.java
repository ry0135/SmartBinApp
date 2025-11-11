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

import java.util.List;

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

        holder.tvNote.setText(item.getNote() != null ? item.getNote() : "Không có ghi chú");
        holder.tvPriority.setText("Độ ưu tiên: " + item.getMinPriority());

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
                    color = Color.parseColor("#4CAF50");
                    break;
                case "OPEN":
                    statusVi = "Đang mở";
                    color = Color.parseColor("#FFC107");
                    break;
                case "DOING":
                    statusVi = "Đang thực hiện";
                    color = Color.parseColor("#9C27B0");
                    break;
                case "CANCELLED":
                    statusVi = "Đã hủy";
                    color = Color.parseColor("#F44336");
                    break;
                default:
                    statusVi = "Không xác định (" + normalized + ")";
                    color = Color.GRAY;
                    break;
            }
        }

        holder.tvStatus.setText("Trạng thái: " + statusVi);
        holder.tvStatus.setTextColor(Color.WHITE);
        holder.tvStatus.setBackgroundColor(color);
        holder.tvStatus.setPadding(16, 8, 16, 8);
        holder.tvStatus.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        // Hiển thị nút hành động theo trạng thái
        if ("OPEN".equalsIgnoreCase(status)) {
            holder.btnAction.setVisibility(View.VISIBLE);
            holder.btnAction.setText("Nhận nhiệm vụ");
            holder.btnAction.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else {
            holder.btnAction.setVisibility(View.GONE);
        }

        // Xử lý click nút hành động
        holder.btnAction.setOnClickListener(v -> {
            if ("OPEN".equalsIgnoreCase(status)) {
                showConfirmDialog(item, "DOING", "Nhận nhiệm vụ này?");
            }
        });

        // Click item mở chi tiết
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
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
        TextView tvNote, tvPriority, tvStatus;
        Button btnAction;

        ViewHolder(View v) {
            super(v);
            tvNote = v.findViewById(R.id.tvNote);
            tvPriority = v.findViewById(R.id.tvPriority);
            tvStatus = v.findViewById(R.id.tvStatus);
            btnAction = v.findViewById(R.id.btnAction);
        }
    }
}
