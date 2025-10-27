package com.example.smartbinapp.adapter;

import com.example.smartbinapp.R;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartbinapp.model.TaskSummary;
import java.util.List;

public class TaskSummaryAdapter extends RecyclerView.Adapter<TaskSummaryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(TaskSummary summary);
    }

    private List<TaskSummary> list;
    private OnItemClickListener listener;

    public TaskSummaryAdapter(List<TaskSummary> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_summary, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TaskSummary item = list.get(position);
        holder.tvNote.setText("" + item.getNote());
//        holder.tvBatch.setText("Batch: " + item.getBatchId());
//        holder.tvCount.setText("Số task: " + item.getTaskCount());
        holder.tvPriority.setText("Độ ưu tiên: " + item.getMinPriority());

        Log.d("TaskStatus", "Status from API: " + item.getStatus());

        String status = item.getStatus();
        String statusVi;

        if (status == null) {
            statusVi = "Không xác định";
        } else {
            switch (status.toUpperCase()) {
                case "COMPLETED":
                    statusVi = "Đã hoàn thành";
                    break;
                case "OPEN":
                    statusVi = "Đang chờ xử lý";
                    break;
                case "CANCELLED":
                    statusVi = "Đã hủy";
                    break;
                default:
                    statusVi = "Không xác định";
                    break;
            }
        }

        holder.tvStatus.setText("Trạng thái: " + statusVi);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBatch, tvNote, tvPriority,tvStatus;
        ViewHolder(View v) {
            super(v);
            tvNote = v.findViewById(R.id.tvNote);
            tvPriority = v.findViewById(R.id.tvPriority);
            tvStatus = v.findViewById(R.id.tvStatus);
        }
    }
}
