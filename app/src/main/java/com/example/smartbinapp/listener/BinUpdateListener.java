package com.example.smartbinapp.listener;


import com.example.smartbinapp.model.Bin;
import com.example.smartbinapp.model.Task;

public interface BinUpdateListener {
    void onBinUpdated(Bin updatedBin);
}