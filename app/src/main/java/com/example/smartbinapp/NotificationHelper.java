package com.example.smartbinapp;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

public class NotificationHelper {
    
    public static void showCustomNotification(Activity activity, String message) {
        // Create custom toast
        LayoutInflater inflater = activity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.notification_layout, 
                (ViewGroup) activity.findViewById(R.id.notification_text));
        
        TextView text = layout.findViewById(R.id.notification_text);
        text.setText(message);
        
        Toast toast = new Toast(activity.getApplicationContext());
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        
        // Animate the notification
        layout.setAlpha(0f);
        layout.setTranslationY(-100f);
        
        toast.show();
        
        // Start animation
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(layout, "alpha", 0f, 1f);
        fadeIn.setDuration(500);
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
        
        ObjectAnimator slideDown = ObjectAnimator.ofFloat(layout, "translationY", -100f, 0f);
        slideDown.setDuration(500);
        slideDown.setInterpolator(new AccelerateDecelerateInterpolator());
        
        fadeIn.start();
        slideDown.start();
    }
    
    public static void showSuccessNotification(Activity activity, String message) {
        showCustomNotification(activity, "✓ " + message);
    }
    
    public static void showErrorNotification(Activity activity, String message) {
        showCustomNotification(activity, "✗ " + message);
    }
    
    public static void showInfoNotification(Activity activity, String message) {
        showCustomNotification(activity, "ℹ " + message);
    }
} 