package com.example.smartbinapp.model;

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class Notification {

    @SerializedName("notificationID")
    private int notificationID;

    @SerializedName("senderId")
    private int senderId;

    @SerializedName("receiverId")
    private int receiverId;

    @SerializedName("type")
    private String type;
    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("read")
    private boolean isRead;

    // ----- Constructors -----
    public Notification() {}


    public Notification(int notificationID, int senderId, int receiverId, String title, String message, String imageUrl, String createdAt, boolean isRead) {
        this.notificationID = notificationID;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.title = title;
        this.message = message;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.isRead = isRead;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getNotificationID() {
        return notificationID;
    }

    public void setNotificationID(int notificationID) {
        this.notificationID = notificationID;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getImageUrl() {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return "https://thumbs.dreamstime.com/b/reminders-icon-often-featuring-bell-symbolizes-alerts-task-management-notifications-reminders-icon-often-featuring-327042080.jpg"; // fallback icon


        }
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    // ----- Helper (optional) -----
    public String getShortTime() {
        // Giúp hiển thị thời gian ngắn gọn (ví dụ: "2h trước", "3 ngày trước")
        if (createdAt == null) return "";
        return createdAt.replace("T", " ").substring(0, 16); // yyyy-MM-dd HH:mm
    }

    // Format thời gian đẹp hơn
    public String getFormattedTime() {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return output.format(input.parse(createdAt));
        } catch (Exception e) {
            return createdAt;
        }
    }
}
