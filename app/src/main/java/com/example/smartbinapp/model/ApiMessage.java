package com.example.smartbinapp.model;

public class ApiMessage {
    private String code;     // Ví dụ: "CREATED", "EMAIL_REGISTERED", "SUCCESS"
    private String message;  // Nội dung thông báo

    public ApiMessage() {
    }

    public ApiMessage(String code, String message) {
        this.code = code;
        this.message = message;
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
