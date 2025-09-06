package com.example.smartbinapp.model;



import java.util.Date;


public class Bin {


    private int binId;

    private String binCode;

    private String street;

    private String ward;

    private String city;

    private double latitude;

    private double longitude;

    private double capacity;

    private double currentFill = 0;

    private int status = 1; // 1 = hoạt động, 0 = bảo trì, 2 = đầy

    private long lastUpdated;

    // Getters và Setters
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

    public String getWard() {
        return ward;
    }
    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
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
    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public double getCurrentFill() {
        return currentFill;
    }
    public void setCurrentFill(double currentFill) {
        this.currentFill = currentFill;
    }

    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }

    public Long getLastUpdated() {
        return lastUpdated;
    }
    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

}
