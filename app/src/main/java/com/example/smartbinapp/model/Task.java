package com.example.smartbinapp.model;


import java.util.Date;

public class Task {
    private int taskID;
    private String taskType;
    private int priority;
    private String status;
    private String notes;
    private String batchId;
    private Bin bin;

    private Date createdAt;   // để Gson parse JSON ISO8601
    private Date completedAt;
    private Double completedLat;
    private Double completedLng;
    private Account assignedTo; // thêm vào để khớp JSON backend

    private String beforeImage;
    private String afterImage;

    private String collectedVolume;
    // Getter & Setter
    public int getTaskID() { return taskID; }
    public void setTaskID(int taskID) { this.taskID = taskID; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public Bin getBin() { return bin; }
    public void setBin(Bin bin) { this.bin = bin; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    public String getBeforeImage() { return beforeImage; }
    public void setBeforeImage(String beforeImage) { this.beforeImage = beforeImage; }

    public Double getCompletedLat() {
        return completedLat;
    }

    public void setCompletedLat(Double completedLat) {
        this.completedLat = completedLat;
    }

    public Double getCompletedLng() {
        return completedLng;
    }

    public void setCompletedLng(Double completedLng) {
        this.completedLng = completedLng;
    }

    public String getAfterImage() { return afterImage; }
    public void setAfterImage(String afterImage) { this.afterImage = afterImage; }

    public String getCollectedVolume() {
        return collectedVolume;
    }

    public void setCollectedVolume(String collectedVolume) {
        this.collectedVolume = collectedVolume;
    }

    public Account getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Account assignedTo) {
        this.assignedTo = assignedTo;
    }
}
