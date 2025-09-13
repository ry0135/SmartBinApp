package com.example.smartbinapp.model;

public class Province {
    private int provinceId;

    private String provinceName;

    // Getter & Setter
    public int getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(int provinceId) {
        this.provinceId = provinceId;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    @Override
    public String toString() {
        return provinceName; // để hiển thị trong Spinner
    }

}
