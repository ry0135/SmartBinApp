package com.example.smartbinapp.network;

import java.util.List;
import java.util.Map;
import com.example.smartbinapp.model.Province;
import com.example.smartbinapp.model.Ward;
import com.example.smartbinapp.model.Account;
import com.example.smartbinapp.model.Bin;
import com.example.smartbinapp.model.LoginRequest;
import com.example.smartbinapp.model.LoginResponse;
import com.example.smartbinapp.model.UpdateProfileResponse;
import com.example.smartbinapp.model.Report;
import com.example.smartbinapp.model.ReportRequest;
import com.example.smartbinapp.model.Feedback;
import com.example.smartbinapp.model.FeedbackStats;
import com.example.smartbinapp.model.ApiResponse;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
//    @GET("api/accounts")
//    Call<List<Account>> getAccounts();
//
//    @POST("api/accounts")
//    Call<Account> createAccount(@Body Account account);
//
//    @GET("api/accounts/{userID}")
//    Call<Account> getAccountById(@Path("userID") String userID);

    @POST("api/accounts/register")
    Call<Account> register(@Body Account account);

    @POST("api/accounts/verificode")
    Call<ResponseBody> verifyCode(@Body Account request);

    @POST("api/accounts/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);
    
    // Raw response for login to handle wrapped JSON
    @POST("api/accounts/login")
    Call<ResponseBody> loginRaw(@Body LoginRequest loginRequest);

    @GET("api/bins")
    Call<List<Bin>> getAllBins();
    
    // Alternative API endpoints that return wrapped responses
    @GET("api/bins")
    Call<ApiResponse<Bin>> getAllBinsWrapped();
    
    // Raw response to debug actual format
    @GET("api/bins")
    Call<ResponseBody> getAllBinsRaw();
    
    @POST("api/accounts/update/{userId}")
    Call<UpdateProfileResponse> updateAccount(@Path("userId") String userId, @Body Account account);

    @GET("api/provinces")
    Call<List<Province>> getProvinces();

    @GET("api/wards")
    Call<List<Ward>> getWards(@Query("provinceId") int provinceId);
    
    // API cho chức năng xem thùng rác gần nhất
    @GET("api/bins/nearby")
    Call<List<Bin>> getNearbyBins(
        @Query("latitude") double latitude,
        @Query("longitude") double longitude
    );
    
    // Raw response for nearby bins to handle server format
    @GET("api/bins/nearby")
    Call<ResponseBody> getNearbyBinsRaw(
        @Query("latitude") double latitude,
        @Query("longitude") double longitude,
        @Query("radius") int radius
    );
    
    // New endpoint for Hoi An nearby bins
    @GET("api/app/bins/nearby/hoian")
    Call<ResponseBody> getNearbyBinsHoiAn();
    
    // API cho chức năng báo thùng đầy/tràn
    @POST("api/reports/create")
    Call<Report> createReport(@Body ReportRequest request);
    
    @Multipart
    @POST("api/reports/upload-image")
    Call<ResponseBody> uploadReportImage(
        @Part MultipartBody.Part image,
        @Query("reportId") Integer reportId
    );
    
    // API cho chức năng theo dõi xử lý phản ánh
    @GET("api/app/reports/user/{userId}")
    Call<List<Report>> getUserReports(@Path("userId") String userId);

    // Raw responses to handle wrapped formats { status, message, data }
    @GET("api/app/reports/user/{userId}")
    Call<ResponseBody> getUserReportsRaw(@Path("userId") String userId);
    
    // Fallback endpoint cũ
    @GET("api/reports/user/{userId}")
    Call<List<Report>> getUserReportsOld(@Path("userId") String userId);
    
    @GET("api/reports/user/{userId}")
    Call<ResponseBody> getUserReportsOldRaw(@Path("userId") String userId);
    
    @GET("api/app/reports/{reportId}")
    Call<Report> getReportDetails(@Path("reportId") int reportId);
    
    // API cho chức năng chấm điểm/đánh giá
    @POST("api/feedbacks/create")
    Call<Feedback> createFeedback(@Body Feedback feedback);
    
    @GET("api/feedbacks/stats/ward/{wardId}")
    Call<FeedbackStats> getFeedbackStats(@Path("wardId") int wardId);
}