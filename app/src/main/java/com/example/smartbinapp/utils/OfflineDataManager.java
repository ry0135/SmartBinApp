package com.example.smartbinapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.smartbinapp.model.ReportRequest;
import com.example.smartbinapp.model.Feedback;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class OfflineDataManager {
    private static final String PREF_OFFLINE_REPORTS = "OfflineReports";
    private static final String PREF_OFFLINE_FEEDBACKS = "OfflineFeedbacks";
    private static final String KEY_REPORTS_LIST = "reports_list";
    private static final String KEY_FEEDBACKS_LIST = "feedbacks_list";
    
    private Context context;
    private Gson gson;
    
    public OfflineDataManager(Context context) {
        this.context = context;
        this.gson = new Gson();
    }
    
    // Report methods
    public void saveReportOffline(ReportRequest report) {
        try {
            List<ReportRequest> reports = getOfflineReports();
            reports.add(report);
            
            SharedPreferences prefs = context.getSharedPreferences(PREF_OFFLINE_REPORTS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_REPORTS_LIST, gson.toJson(reports));
            editor.apply();
            
        } catch (Exception e) {
        }
    }
    
    public List<ReportRequest> getOfflineReports() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_OFFLINE_REPORTS, Context.MODE_PRIVATE);
            String reportsJson = prefs.getString(KEY_REPORTS_LIST, "[]");
            
            Type listType = new TypeToken<List<ReportRequest>>(){}.getType();
            List<ReportRequest> reports = gson.fromJson(reportsJson, listType);
            
            return reports != null ? reports : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    public void removeOfflineReport(ReportRequest report) {
        try {
            List<ReportRequest> reports = getOfflineReports();
            reports.remove(report);
            
            SharedPreferences prefs = context.getSharedPreferences(PREF_OFFLINE_REPORTS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_REPORTS_LIST, gson.toJson(reports));
            editor.apply();
            
        } catch (Exception e) {
        }
    }
    
    // Feedback methods
    public void saveFeedbackOffline(Feedback feedback) {
        try {
            List<Feedback> feedbacks = getOfflineFeedbacks();
            feedbacks.add(feedback);
            
            SharedPreferences prefs = context.getSharedPreferences(PREF_OFFLINE_FEEDBACKS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_FEEDBACKS_LIST, gson.toJson(feedbacks));
            editor.apply();
            
        } catch (Exception e) {
        }
    }
    
    public List<Feedback> getOfflineFeedbacks() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_OFFLINE_FEEDBACKS, Context.MODE_PRIVATE);
            String feedbacksJson = prefs.getString(KEY_FEEDBACKS_LIST, "[]");
            
            Type listType = new TypeToken<List<Feedback>>(){}.getType();
            List<Feedback> feedbacks = gson.fromJson(feedbacksJson, listType);
            
            return feedbacks != null ? feedbacks : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    public void removeOfflineFeedback(Feedback feedback) {
        try {
            List<Feedback> feedbacks = getOfflineFeedbacks();
            feedbacks.remove(feedback);
            
            SharedPreferences prefs = context.getSharedPreferences(PREF_OFFLINE_FEEDBACKS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_FEEDBACKS_LIST, gson.toJson(feedbacks));
            editor.apply();
            
        } catch (Exception e) {
        }
    }
    
    // Utility methods
    public int getOfflineReportsCount() {
        return getOfflineReports().size();
    }
    
    public int getOfflineFeedbacksCount() {
        return getOfflineFeedbacks().size();
    }
    
    public void clearAllOfflineData() {
        try {
            SharedPreferences reportsPrefs = context.getSharedPreferences(PREF_OFFLINE_REPORTS, Context.MODE_PRIVATE);
            SharedPreferences feedbacksPrefs = context.getSharedPreferences(PREF_OFFLINE_FEEDBACKS, Context.MODE_PRIVATE);
            
            reportsPrefs.edit().clear().apply();
            feedbacksPrefs.edit().clear().apply();
            
        } catch (Exception e) {
        }
    }
}
