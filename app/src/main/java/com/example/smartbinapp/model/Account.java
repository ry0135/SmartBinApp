package com.example.smartbinapp.model;

import java.util.Date;

public class Account {
    private Integer accountId;
    private String fullName;
    private String email;
    private String password;
    private String phone;

    private int role; // 1 = Admin, 2 = Nhân viên, 3 = Người dân
    private int status; // 1 = hoạt động, 0 = khóa
    private String address; 
    private String code;
    private Integer wardId; // WardID để tham chiếu đến bảng Wards
    private Date createdAt;
    private Boolean IsVerified; // true = đã xác thực, false = chưa xác thực

    public Account() {
    }

    public Account(String email, String code) {
        this.email = email;
        this.code = code;
    }

    public Account(String fullName, String email, String password, String phone, int role, int status, String address, Date createdAt) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.address = address;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getIsVerified() {
        return IsVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        IsVerified = isVerified;
    }

    public Integer getWardId() {
        return wardId;
    }

    public void setWardId(Integer wardId) {
        this.wardId = wardId;
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
                ", address='" + address + '\'' +
                ", code='" + code + '\'' +
                ", wardId=" + wardId +
                ", createdAt=" + createdAt +
                ", IsVerified=" + IsVerified +
                '}';
    }
}
