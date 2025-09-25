package com.example.smartbinapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbinapp.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter để hiển thị danh sách hình ảnh báo cáo
 */
public class ReportImagesAdapter extends RecyclerView.Adapter<ReportImagesAdapter.ImageViewHolder> {
    private List<String> imageUrls = new ArrayList<>();
    private AppCompatActivity activity;

    public ReportImagesAdapter(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        // Hiển thị hình ảnh placeholder
        holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        
        // Trong ứng dụng thực tế, bạn sẽ sử dụng Glide hoặc Picasso để tải hình ảnh
        // Ví dụ với Glide (đã được thêm vào build.gradle):
        /*
        Glide.with(activity)
            .load(imageUrls.get(position))
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .into(holder.imageView);
        */
    }

    @Override
    public int getItemCount() {
        return imageUrls != null ? imageUrls.size() : 0;
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                imageView = itemView.findViewById(R.id.ivReportImage);
            } catch (Exception e) {
                System.out.println("Error finding image view: " + e.getMessage());
            }
        }
    }
}


