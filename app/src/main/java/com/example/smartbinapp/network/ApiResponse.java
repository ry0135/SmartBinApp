package com.example.smartbinapp.network;

import com.example.smartbinapp.model.Account;

public class ApiResponse {
    private boolean success;
    private String message;
    private Account data;

    // Constructors
    public ApiResponse() {}

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public ApiResponse(boolean success, String message, Account data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Getters và Setters
    public boolean isSuccess() { 
        return success; 
    }
    
    public void setSuccess(boolean success) { 
        this.success = success; 
    }
    
    public String getMessage() { 
        return message; 
    }
    
    public void setMessage(String message) { 
        this.message = message; 
    }
    
    public Account getData() { 
        return data; 
    }
    
    public void setData(Account data) { 
        this.data = data; 
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
