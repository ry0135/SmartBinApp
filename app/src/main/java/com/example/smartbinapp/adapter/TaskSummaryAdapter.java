package com.example.smartbinapp.adapter;

import com.example.smartbinapp.R;
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

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBatch, tvNote, tvPriority;
        ViewHolder(View v) {
            super(v);
            tvNote = v.findViewById(R.id.tvNote);
            tvPriority = v.findViewById(R.id.tvPriority);
        }
    }
}
