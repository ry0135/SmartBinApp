package com.example.smartbinapp.model;

public class Feedback {
    private int feedbackId;      // Khớp với database: FeedbackID INT IDENTITY(1,1) PRIMARY KEY
    private int accountId;       // Khớp với database: AccountID INT NOT NULL
    private int wardId;          // Khớp với database: WardID INT NOT NULL
    private int rating;          // Khớp với database: Rating INT NOT NULL CHECK (Rating BETWEEN 1 AND 5)
    private String comment;      // Khớp với database: Comment NVARCHAR(500)
    private int reportId;        // Khớp với database: ReportID INT NULL
    private String createdAt;    // Khớp với database: CreatedAt DATETIME DEFAULT GETDATE()

    // Constructors
    public Feedback() {}

    public Feedback(int accountId, int wardId, int rating, String comment, int reportId) {
        this.accountId = accountId;
        this.wardId = wardId;
        this.rating = rating;
        this.comment = comment;
        this.reportId = reportId;
    }

    // Getters and Setters
    public int getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(int feedbackId) {
        this.feedbackId = feedbackId;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public int getWardId() {
        return wardId;
    }

    public void setWardId(int wardId) {
        this.wardId = wardId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getReportId() {
        return reportId;
    }

    public void setReportId(int reportId) {
        this.reportId = reportId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    // Validation methods - khớp với database constraints
    public boolean isValid() {
        return accountId > 0 &&                    // AccountID INT NOT NULL
               wardId > 0 &&                       // WardID INT NOT NULL
               rating >= 1 && rating <= 5 &&      // Rating INT NOT NULL CHECK (Rating BETWEEN 1 AND 5)
               comment != null && !comment.trim().isEmpty(); // Comment không được null/empty
    }

    public String getValidationError() {
        if (accountId <= 0) {
            return "Account ID is required and must be positive";
        }
        if (wardId <= 0) {
            return "Ward ID is required and must be positive";
        }
        if (rating < 1 || rating > 5) {
            return "Rating must be between 1 and 5";
        }
        if (comment == null || comment.trim().isEmpty()) {
            return "Comment is required";
        }
        if (comment.length() > 500) {
            return "Comment must not exceed 500 characters";
        }
        return null;
    }

    @Override
    public String toString() {
        return "Feedback{" +
                "feedbackId=" + feedbackId +
                ", accountId=" + accountId +
                ", wardId=" + wardId +
                ", rating=" + rating +
                ", comment='" + comment + '\'' +
                ", reportId=" + reportId +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}