package com.example.smartbinapp.model;

public class TaskSummary {
    private String batchId;
    private int assignedTo;
    private String note;
    private int minPriority;
    private String status;   // NEW FIELD

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public int getAssignedTo() { return assignedTo; }
    public void setAssignedTo(int assignedTo) { this.assignedTo = assignedTo; }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getMinPriority() { return minPriority; }
    public void setMinPriority(int minPriority) { this.minPriority = minPriority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
