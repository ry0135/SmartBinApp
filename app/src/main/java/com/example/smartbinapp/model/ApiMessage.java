package com.example.smartbinapp.model;

public class ApiMessage {
    private String code;     // Ví dụ: "CREATED", "EMAIL_REGISTERED", "SUCCESS"
    private String message;  // Nội dung thông báo
    private Integer userId;
    private Integer role;
    public ApiMessage() {
    }

    public ApiMessage(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setuserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    // Getter & Setter
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
