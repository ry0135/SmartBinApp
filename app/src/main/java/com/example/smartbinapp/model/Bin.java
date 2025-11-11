package com.example.smartbinapp.model;

import com.google.gson.annotations.SerializedName; // THÊM DÒNG NÀY
import java.util.Date;

public class Bin {

    // 1. Ánh xạ trường JSON "binID" vào biến "binId"
    @SerializedName(value = "binId", alternate = {"binID"})
    private int binId; // Dùng camelCase cho đúng chuẩn Java
    @SerializedName("binCode")
    private String binCode;

    @SerializedName("street")
    private String street;

    @SerializedName("wardID")
    private int wardID;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("capacity")
    private double capacity;

    @SerializedName("currentFill")
    private double currentFill;

    @SerializedName("status")
    private int status; // Sửa thành int để khớp với JSON (2)

    @SerializedName("lastUpdated")
    private Object lastUpdatedRaw; // 👈 Giữ nguyên kiểu Object để tránh Gson lỗi

    // 👇 Getter thủ công convert sang Date
    public Date getLastUpdated() {
        if (lastUpdatedRaw == null) return null;
        try {
            if (lastUpdatedRaw instanceof Number) {
                long timestamp = ((Number) lastUpdatedRaw).longValue();
                return new Date(timestamp);
            }
            if (lastUpdatedRaw instanceof String) {
                long timestamp = Long.parseLong((String) lastUpdatedRaw);
                return new Date(timestamp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 👇 Setter cho Gson
    public void setLastUpdated(Object lastUpdatedRaw) {
        this.lastUpdatedRaw = lastUpdatedRaw;
    }

    // Các trường này không có trong JSON gốc nhưng được thêm vào từ ward/province lồng nhau
    private String wardName;
    private String provinceName;

    // Default constructor
    public Bin() {}

    // Getters & Setters (Tất cả đều dùng binId viết thường)
    public int getBinId() {
        return binId;
    }

    public void setBinId(int binId) {
        this.binId = binId;
    }

    public String getBinCode() {
        return binCode;
    }

    public void setBinCode(String binCode) {
        this.binCode = binCode;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public int getWardID() {
        return wardID;
    }

    public void setWardID(int wardID) {
        this.wardID = wardID;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getCapacity() {
        return capacity;
    }

    // Constructor for fallback data
    public Bin(int binId, String binCode, double latitude, double longitude, int status, double currentFill, String street) {
        this.binId = binId;
        this.binCode = binCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.currentFill = currentFill;
        this.street = street;
        this.capacity = 100.0; // Default capacity
        this.wardID = 1; // Default ward
        this.lastUpdatedRaw = System.currentTimeMillis(); // epoch time hiện tại
        this.wardName = "Unknown";
        this.provinceName = "Unknown";
    }


    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public double getCurrentFill() {
        return currentFill;
    }

    public void setCurrentFill(double currentFill) {
        this.currentFill = currentFill;
    }

//    public Date getLastUpdated() {
//        return lastUpdated;
//    }
//
//    public void setLastUpdated(Date lastUpdated) {
//        this.lastUpdated = lastUpdated;
//    }


    public String getWardName() {
        return wardName;
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }


    public void setWardName(String wardName) {
        this.wardName = wardName;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }
}
