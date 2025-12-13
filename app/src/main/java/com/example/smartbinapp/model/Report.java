package com.example.smartbinapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

public class Report {
    private Integer reportId;
    @SerializedName(value = "binId", alternate = {"BinID", "binID", "BinId"})

    private Integer binID;
    private Integer accountId;
    private String reportType;
    private String description;
    private String status;
    private Integer assignedTo;
    private Integer taskId;
    private Date createdAt;
    private Date updatedAt;
    private Date resolvedAt;
    private List<String> images;

    private boolean reviewed;

    @SerializedName("binCode")
    private String binCode;

    @SerializedName("binAddress")
    private String binAddress;
    public Report() {
    }

    public Integer getBinID() {
        return binID;
    }

    public void setBinID(Integer binID) {
        this.binID = binID;
    }

    public boolean isReviewed() {
        return reviewed;
    }

    public void setReviewed(boolean reviewed) {
        this.reviewed = reviewed;
    }

    // Getters và Setters
    public Integer getReportId() {
        return reportId;
    }

    public void setReportId(Integer reportId) {
        this.reportId = reportId;
    }

    public Integer getBinId() {
        return binID;
    }

    public void setBinId(Integer binId) {
        this.binID = binId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Integer assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Date resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getBinCode() {
        return binCode;
    }

    public void setBinCode(String binCode) {
        this.binCode = binCode;
    }

    public String getBinAddress() {
        return binAddress;
    }

    public void setBinAddress(String binAddress) {
        this.binAddress = binAddress;
    }

    // Helper method để lấy tên trạng thái tiếng Việt
    public String getStatusVietnamese() {
        if (status == null) {
            return "Không xác định";
        }
        
        switch (status) {
            case "RECEIVED":
                return "Đã tiếp nhận";
            case "IN_PROGRESS":
                return "Đang xử lý";
            case "RESOLVED":
                return "Đã xử lý ";
            case "DONE":
                return "Hoàn thành";
            case "CANCELLED":
                return "Đã hủy";
            default:
                return status;
        }
    }
}
