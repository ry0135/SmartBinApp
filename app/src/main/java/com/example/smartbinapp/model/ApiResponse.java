package com.example.smartbinapp.model;

import java.util.List;

public class ApiResponse<T> {
    private String status;
    private String message;
    private T data;
    private List<T> items; // For array responses
    private List<T> results; // Alternative field name

    public ApiResponse() {}

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

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public List<T> getResults() {
        return results;
    }

    public void setResults(List<T> results) {
        this.results = results;
    }

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status) || "ok".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", items=" + items +
                '}';
    }
}
