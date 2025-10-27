package com.example.smartbinapp.network; // Giữ nguyên package của bạn

import com.google.gson.annotations.SerializedName;

/**
 * Một lớp Generic (tổng quát) để xử lý tất cả các phản hồi từ API.
 * Nó có thể chứa bất kỳ loại dữ liệu nào (Account, Report, List<Bin>...)
 * bằng cách sử dụng kiểu <T>.
 */
public class ApiResponse<T> {

    // 1. Sửa 'success' (boolean) thành 'status' (String) để khớp với JSON
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    // 2. Sửa 'Account' thành kiểu Generic 'T' để có thể chứa bất kỳ đối tượng nào
    @SerializedName("data")
    private T data;

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
