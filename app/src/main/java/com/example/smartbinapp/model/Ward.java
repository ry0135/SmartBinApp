package com.example.smartbinapp.model;

public class Ward {
    private int wardId;
    private String wardName;

    // Getter & Setter
    public int getWardId() {
        return wardId;
    }
    public void setWardId(int wardId) {
        this.wardId = wardId;
    }

    public String getWardName() {
        return wardName;
    }
    public void setWardName(String wardName) {
        this.wardName = wardName;
    }

    @Override
    public String toString() {
        return wardName;
    }
}
