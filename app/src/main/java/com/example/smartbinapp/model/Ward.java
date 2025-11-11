package com.example.smartbinapp.model;

import com.google.gson.annotations.SerializedName;

public class Ward {
    @SerializedName("wardId")
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
