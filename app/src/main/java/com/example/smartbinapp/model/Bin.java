package com.example.smartbinapp.model;

import java.util.Date;

public class Bin {
    private int binId;
    private String binCode;
    private String street;
    private int wardID;
    private double latitude;
    private double longitude;
    private double capacity;
    private double currentFill;
    private String status;
    private Date lastUpdated;
    private String wardName;
    private String provinceName;

    // Default constructor
    public Bin() {}

    // Constructor for fallback data
    public Bin(int binId, String binCode, double latitude, double longitude, String status, double currentFill, String street) {
        this.binId = binId;
        this.binCode = binCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.currentFill = currentFill;
        this.street = street;
        this.capacity = 100.0; // Default capacity
        this.wardID = 1; // Default ward
        this.lastUpdated = new Date();
        this.wardName = "Unknown";
        this.provinceName = "Unknown";
    }

    // Getters & Setters
    public int getBinId() { return binId; }
    public void setBinId(int binId) { this.binId = binId; }

    public String getBinCode() { return binCode; }
    public void setBinCode(String binCode) { this.binCode = binCode; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public int getWardID() { return wardID; }
    public void setWardID(int wardID) { this.wardID = wardID; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getCapacity() { return capacity; }
    public void setCapacity(double capacity) { this.capacity = capacity; }

    public double getCurrentFill() { return currentFill; }
    public void setCurrentFill(double currentFill) { this.currentFill = currentFill; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }

    // 🔹 Getter Setter mới
    public String getWardName() { return wardName; }
    public void setWardName(String wardName) { this.wardName = wardName; }

    public String getProvinceName() { return provinceName; }
    public void setProvinceName(String provinceName) { this.provinceName = provinceName; }
}
