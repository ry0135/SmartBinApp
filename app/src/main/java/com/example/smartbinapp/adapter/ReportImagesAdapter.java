package com.example.smartbinapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smartbinapp.R;

import java.util.ArrayList;
import java.util.List;

public class ReportImagesAdapter extends RecyclerView.Adapter<ReportImagesAdapter.ImageViewHolder> {

    private List<String> imageUrls = new ArrayList<>();
    private Context context;

    public ReportImagesAdapter(Context context) {
        this.context = context;
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

        String url = imageUrls.get(position);

        Glide.with(context)
                .load(url)
                .placeholder(R.drawable.placeholder_image)  // tự chọn
                .error(R.drawable.error_image)              // tự chọn
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivReportImage);
        }
    }
}
