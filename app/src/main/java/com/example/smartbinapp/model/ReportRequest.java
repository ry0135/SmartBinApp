package com.example.smartbinapp.model;

public class ReportRequest {
    // 1. Đồng bộ kiểu dữ liệu với server: private Integer userId;
    private Integer accountId;
    private int binId;
    private String reportType;
    private String description;
    private String location;
    private double latitude;
    private double longitude;
    private String status;

    // Constructors
    public ReportRequest() {}

    public ReportRequest(Integer accountId, int binId, String reportType, String description,
                         String location, double latitude, double longitude) {
        this.accountId= accountId;
        this.binId = binId;
        this.reportType = reportType;
        this.description = description;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = "PENDING";
    }

    // Getters and Setters

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }


    // 2. Sửa lại setter để nhận đúng kiểu Integer


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

    // --- CÁC PHƯƠNG THỨC VALIDATION ĐÃ SỬA ---

    /**
     * Kiểm tra xem các trường bắt buộc của request có hợp lệ không.
     * Phương thức này nên được gọi ở client trước khi gửi request.
     * @return true nếu hợp lệ, ngược lại false.
     */
    public boolean isValid() {
        // 3. Sửa lại logic validation cho chặt chẽ
        return accountId != null && accountId > 0 &&
                binId > 0 && // binId phải là số dương, không chấp nhận 0
                reportType != null && !reportType.trim().isEmpty() &&
                description != null && !description.trim().isEmpty() &&
                location != null && !location.trim().isEmpty();
    }

    /**
     * Trả về thông báo lỗi đầu tiên tìm thấy.
     * @return Một chuỗi mô tả lỗi, hoặc null nếu không có lỗi.
     */
    // 4. Sửa lại kiểu trả về thành String
    public String getValidationError() {
        if (accountId == null || accountId <= 0) {
            return "User ID không hợp lệ";
        }
        if (binId <= 0) { // Sửa lại kiểm tra
            return "Mã thùng rác không hợp lệ";
        }
        if (reportType == null || reportType.trim().isEmpty()) {
            return "Loại báo cáo không được để trống";
        }
        if (description == null || description.trim().isEmpty()) {
            return "Mô tả không được để trống";
        }
        if (location == null || location.trim().isEmpty()) {
            return "Vị trí không được để trống";
        }
        return null; // Trả về null nếu không có lỗi
    }

    @Override
    public String toString() {
        return "ReportRequest{" +
                "accountId=" + accountId + // Bỏ dấu nháy đơn cho kiểu số
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
