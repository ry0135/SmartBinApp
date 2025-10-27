package com.example.smartbinapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Account {
    @SerializedName(value = "accountId", alternate = {"userId"})
    private Integer accountId;
    @SerializedName("fullName")
    private String fullName;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("phone")
    private String phone;

    @SerializedName("role")
    private int role;

    @SerializedName("status")
    private int status;

    // ⚠️ Đúng JSON thực tế backend trả về là "addressDetail"
    @SerializedName("addressDetail")
    private String addressDetail;

    @SerializedName("code")
    private String code;

    // ⚠️ JSON backend trả về "wardID", không phải "wardId"
    @SerializedName("wardID")
    private Integer wardID;

    // ⚠️ JSON backend dùng "isVerified" viết thường “i”
    @SerializedName("isVerified")
    private Boolean isVerified;

    @SerializedName("fcmToken")
    private String fcmToken;
    private Date createdAt;
    private Boolean IsVerified; // true = đã xác thực, false = chưa xác thực
    private String avatarUrl;

    public Account() {
    }

    public Account(String email, String code) {
        this.email = email;
        this.code = code;
    }

    public Account(String fullName, String email, String password, String phone, int role, int status, String addressDetail, Date createdAt) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.addressDetail = addressDetail;
        this.createdAt = createdAt;
    }

    // Getters và Setters
    public Integer getAccountId() {
        return accountId;
    }
    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getRole() {
        return role;
    }
    public void setRole(int role) {
        this.role = role;
    }

    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }



    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Boolean getIsVerified() {
        return IsVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        IsVerified = isVerified;
    }

    public Integer getWardId() {
        return wardID;
    }

    public void setWardId(Integer wardId) {
        this.wardID = wardId;
    }

    public String getAddressDetail() {
        return addressDetail;
    }

    public void setAddressDetail(String addressDetail) {
        this.addressDetail = addressDetail;
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId=" + accountId +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", phone='" + phone + '\'' +
                ", role=" + role +
                ", status=" + status +

                ", code='" + code + '\'' +
                ", wardId=" + wardID +
                ", createdAt=" + createdAt +
                ", IsVerified=" + IsVerified +
                '}';
    }
}
