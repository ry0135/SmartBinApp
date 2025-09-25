package com.example.smartbinapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbinapp.R;
import com.example.smartbinapp.model.Report;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResolvedReportsAdapter extends RecyclerView.Adapter<ResolvedReportsAdapter.ReportViewHolder> {

    private List<Report> reports;
    private OnReportClickListener listener;

    public interface OnReportClickListener {
        void onReportClick(Report report);
    }

    public ResolvedReportsAdapter(List<Report> reports, OnReportClickListener listener) {
        this.reports = reports;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resolved_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reports.get(position);
        holder.bind(report, listener);
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        private TextView tvReportId, tvDescription, tvReportType, tvStatus, tvCreatedAt, tvResolvedAt;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReportId = itemView.findViewById(R.id.tvReportId);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvReportType = itemView.findViewById(R.id.tvReportType);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            tvResolvedAt = itemView.findViewById(R.id.tvResolvedAt);
        }

        public void bind(Report report, OnReportClickListener listener) {
            tvReportId.setText("Báo cáo #" + report.getReportId());
            tvDescription.setText(report.getDescription());
            tvReportType.setText("Loại: " + getReportTypeText(report.getReportType()));
            tvStatus.setText("Trạng thái: " + getStatusText(report.getStatus()));
            
            // Format dates
            tvCreatedAt.setText("Tạo: " + formatDate(report.getCreatedAt()));
            tvResolvedAt.setText("Xử lý: " + formatDate(report.getResolvedAt()));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReportClick(report);
                }
            });
        }

        private String getReportTypeText(String reportType) {
            switch (reportType) {
                case "FULL": return "Thùng đầy";
                case "OVERFLOW": return "Thùng tràn";
                case "DAMAGED": return "Thùng hư hỏng";
                case "OTHER": return "Khác";
                default: return reportType;
            }
        }

        private String getStatusText(String status) {
            switch (status) {
                case "PENDING": return "Chờ xử lý";
                case "IN_PROGRESS": return "Đang xử lý";
                case "RESOLVED": return "Đã xử lý";
                case "REJECTED": return "Từ chối";
                default: return status;
            }
        }

        private String formatDate(Date date) {
            if (date == null) {
                return "Không xác định";
            }
            try {
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return outputFormat.format(date);
            } catch (Exception e) {
                return "Không xác định";
            }
        }
    }
}
