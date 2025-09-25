package com.example.smartbinapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbinapp.R;
import com.example.smartbinapp.model.Report;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.ReportViewHolder> {

    private List<Report> reports = new ArrayList<>();
    private OnReportClickListener listener;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public interface OnReportClickListener {
        void onReportClick(Report report);
    }

    public ReportsAdapter(Context context, OnReportClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setReports(List<Report> reports) {
        this.reports = reports;
        notifyDataSetChanged();
    }

    public void addReport(Report report) {
        reports.add(report);
        notifyItemInserted(reports.size() - 1);
    }

    public void clearReports() {
        reports.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        holder.bind(reports.get(position));
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvReportType;
        TextView tvStatus;
        TextView tvBinInfo;
        TextView tvDescription;
        TextView tvDate;
        Button btnViewDetail;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReportType = itemView.findViewById(R.id.tvReportType);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvBinInfo = itemView.findViewById(R.id.tvBinInfo);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnViewDetail = itemView.findViewById(R.id.btnViewDetail);

            btnViewDetail.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onReportClick(reports.get(position));
                }
            });

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onReportClick(reports.get(position));
                }
            });
        }

        public void bind(Report report) {
            // Set report type
            String reportType;
            String reportTypeValue = report.getReportType();
            if (reportTypeValue == null) {
                reportType = "Khác";
            } else {
                switch (reportTypeValue) {
                    case "FULL":
                        reportType = "Thùng đầy";
                        break;
                    case "OVERFLOW":
                        reportType = "Thùng tràn";
                        break;
                    case "DAMAGED":
                        reportType = "Thùng hư hỏng";
                        break;
                    default:
                        reportType = "Khác";
                        break;
                }
            }
            tvReportType.setText(reportType);

            // Set status with appropriate color
            tvStatus.setText(report.getStatusVietnamese());
            int statusColor;
            String statusValue = report.getStatus();
            if (statusValue == null) {
                statusColor = android.R.color.darker_gray;
            } else {
                switch (statusValue) {
                    case "RECEIVED":
                        statusColor = android.R.color.holo_blue_dark;
                        break;
                    case "ASSIGNED":
                        statusColor = android.R.color.holo_orange_dark;
                        break;
                    case "PROCESSING":
                        statusColor = android.R.color.holo_orange_light;
                        break;
                    case "DONE":
                        statusColor = android.R.color.holo_green_dark;
                        break;
                    case "CANCELLED":
                        statusColor = android.R.color.darker_gray;
                        break;
                    default:
                        statusColor = android.R.color.holo_blue_dark;
                        break;
                }
            }
            try {
                tvStatus.getBackground().setTint(ContextCompat.getColor(context, statusColor));
            } catch (Exception e) {
                // Xử lý trường hợp background null hoặc không hỗ trợ tint
                tvStatus.setBackgroundColor(ContextCompat.getColor(context, statusColor));
            }

            // Set bin info
            String binInfo = "Mã thùng: " + (report.getBinCode() != null ? report.getBinCode() : "");
            if (report.getBinAddress() != null && !report.getBinAddress().isEmpty()) {
                binInfo += " (" + report.getBinAddress() + ")";
            }
            tvBinInfo.setText(binInfo);

            // Set description (truncated if too long)
            tvDescription.setText(report.getDescription());

            // Set date
            if (report.getCreatedAt() != null) {
                tvDate.setText(dateFormat.format(report.getCreatedAt()));
            } else {
                tvDate.setText("");
            }
        }
    }
}
