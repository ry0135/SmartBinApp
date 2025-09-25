package com.example.smartbinapp.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbinapp.R;

import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<Uri> imageUris = new ArrayList<>();
    private OnImageClickListener listener;

    public interface OnImageClickListener {
        void onImageClick(int position);
    }

    public ImageAdapter(OnImageClickListener listener) {
        this.listener = listener;
    }

    public void setImageUris(List<Uri> imageUris) {
        this.imageUris = imageUris;
        notifyDataSetChanged();
    }

    public void addImage(Uri imageUri) {
        imageUris.add(imageUri);
        notifyItemInserted(imageUris.size() - 1);
    }

    public void removeImage(int position) {
        if (position >= 0 && position < imageUris.size()) {
            imageUris.remove(position);
            notifyItemRemoved(position);
        }
    }

    public List<Uri> getImageUris() {
        return imageUris;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        holder.bind(imageUris.get(position));
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        ImageView ivRemove;

        public ImageViewHolder(@NonNull View itemView, OnImageClickListener listener) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            ivRemove = itemView.findViewById(R.id.ivRemove);

            ivRemove.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onImageClick(position);
                    }
                }
            });
        }

        public void bind(Uri imageUri) {
            ivImage.setImageURI(imageUri);
        }
    }
}


