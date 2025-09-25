package com.example.smartbinapp.model;

public class FeedbackStats {
    private int wardId;
    private String wardName;
    private double averageRating;
    private int totalFeedbacks;
    private int excellentCount;
    private int goodCount;
    private int averageCount;
    private int poorCount;
    private int badCount;

    // Constructors
    public FeedbackStats() {}

    public FeedbackStats(int wardId, String wardName, double averageRating, int totalFeedbacks,
                        int excellentCount, int goodCount, int averageCount, int poorCount, int badCount) {
        this.wardId = wardId;
        this.wardName = wardName;
        this.averageRating = averageRating;
        this.totalFeedbacks = totalFeedbacks;
        this.excellentCount = excellentCount;
        this.goodCount = goodCount;
        this.averageCount = averageCount;
        this.poorCount = poorCount;
        this.badCount = badCount;
    }

    // Getters and Setters
    public int getWardId() {
        return wardId;
    }

    public void setWardId(int wardId) {
        this.wardId = wardId;
    }

    public String getWardName() {
        return wardName;
    }

    public void setWardName(String wardName) {
        this.wardName = wardName;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public int getTotalFeedbacks() {
        return totalFeedbacks;
    }

    public void setTotalFeedbacks(int totalFeedbacks) {
        this.totalFeedbacks = totalFeedbacks;
    }

    public int getExcellentCount() {
        return excellentCount;
    }

    public void setExcellentCount(int excellentCount) {
        this.excellentCount = excellentCount;
    }

    public int getGoodCount() {
        return goodCount;
    }

    public void setGoodCount(int goodCount) {
        this.goodCount = goodCount;
    }

    public int getAverageCount() {
        return averageCount;
    }

    public void setAverageCount(int averageCount) {
        this.averageCount = averageCount;
    }

    public int getPoorCount() {
        return poorCount;
    }

    public void setPoorCount(int poorCount) {
        this.poorCount = poorCount;
    }

    public int getBadCount() {
        return badCount;
    }

    public void setBadCount(int badCount) {
        this.badCount = badCount;
    }

    @Override
    public String toString() {
        return "FeedbackStats{" +
                "wardId=" + wardId +
                ", wardName='" + wardName + '\'' +
                ", averageRating=" + averageRating +
                ", totalFeedbacks=" + totalFeedbacks +
                ", excellentCount=" + excellentCount +
                ", goodCount=" + goodCount +
                ", averageCount=" + averageCount +
                ", poorCount=" + poorCount +
                ", badCount=" + badCount +
                '}';
    }
}