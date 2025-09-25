package com.example.smartbinapp.model;

public class ReportRequest {
    private String userId;
    private int binId;
    private String reportType;
    private String description;
    private String location;
    private double latitude;
    private double longitude;
    private String status;

    // Constructors
    public ReportRequest() {}

    public ReportRequest(String userId, int binId, String reportType, String description, 
                        String location, double latitude, double longitude) {
        this.userId = userId;
        this.binId = binId;
        this.reportType = reportType;
        this.description = description;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = "PENDING";
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getBinId() {
        return binId;
    }

    public void setBinId(int binId) {
        this.binId = binId;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Validation methods
    public boolean isValid() {
        return userId != null && !userId.trim().isEmpty() &&
               binId >= 0 && // Cho phép binId = 0 (không có thùng rác cụ thể)
               reportType != null && !reportType.trim().isEmpty() &&
               description != null && !description.trim().isEmpty() &&
               location != null && !location.trim().isEmpty();
    }

    public String getValidationError() {
        if (userId == null || userId.trim().isEmpty()) {
            return "User ID is required";
        }
        if (binId < 0) {
            return "Valid Bin ID is required";
        }
        if (reportType == null || reportType.trim().isEmpty()) {
            return "Report type is required";
        }
        if (description == null || description.trim().isEmpty()) {
            return "Description is required";
        }
        if (location == null || location.trim().isEmpty()) {
            return "Location is required";
        }
        return null;
    }

    @Override
    public String toString() {
        return "ReportRequest{" +
                "userId='" + userId + '\'' +
                ", binId=" + binId +
                ", reportType='" + reportType + '\'' +
                ", description='" + description + '\'' +
                ", location='" + location + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", status='" + status + '\'' +
                '}';
    }
}
