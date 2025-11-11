package com.example.smartbinapp.network;

import java.util.List;
import java.util.Map;
import com.example.smartbinapp.model.Account;
import com.example.smartbinapp.model.ApiMessage;
import com.example.smartbinapp.model.Bin;
import com.example.smartbinapp.model.Feedback;
import com.example.smartbinapp.model.FeedbackStats;
import com.example.smartbinapp.model.LoginRequest;
import com.example.smartbinapp.model.Notification;
import com.example.smartbinapp.model.Province;
import com.example.smartbinapp.model.Report;
import com.example.smartbinapp.model.ReportRequest;
import com.example.smartbinapp.model.Task;
import com.example.smartbinapp.model.TaskSummary;
import com.example.smartbinapp.model.Ward;
import com.google.gson.JsonObject;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
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
    Call<Account> login(@Body LoginRequest loginRequest);


    @GET("api/accounts/{id}")
    Call<Account> getUserById(@Path("id") String userId);

    @POST("api/accounts/login")
    Call<Account> loginRaw1(@Body LoginRequest loginRequest);
    @POST("auth/login/google")
    Call<ApiMessage> loginWithGoogle(@Body Map<String, String> body);

    // Raw response cho đăng nhập để tự parse JSON bọc {status, message, data}
    @POST("api/accounts/login")
    Call<ResponseBody> loginRaw(@Body LoginRequest loginRequest);


    @GET("api/bins")
    Call<List<Bin>> getAllBins();

    @GET("api/bins/dto")
    Call<List<Bin>> getAllBinDTOs();

    @PUT("api/accounts/update/{id}")
    Call<Account> updateAccount(@Path("id") String id, @Body Account account);


    @GET("api/location/provinces")
    Call<List<Province>> getProvinces();

    @GET("api/location/wards/{provinceId}")
    Call<List<Ward>> getWards(@Path("provinceId") int provinceId);


    @GET("api/tasks/summary/{workerId}")
    Call<List<TaskSummary>> getTaskSummaries(@Path("workerId") int workerId);

    @GET("api/tasks/batch/{workerId}/{batchId}")
    Call<List<Task>> getTasksInBatch(@Path("workerId") int workerId,
                                     @Path("batchId") String batchId);

    @POST("api/accounts/{id}/update-token")
    Call<ApiMessage> updateFcmToken(@Path("id") int workerId, @Body Map<String, String> body);


    @Multipart
    @POST("api/tasks/complete")
    Call<ApiMessage> completeTaskWithImage(
            @Part("taskId") RequestBody taskId,
            @Part("lat") RequestBody lat,
            @Part("lng") RequestBody lng,
            @Part MultipartBody.Part proofImage
    );
    // API cho chức năng xem thùng rác gần nhất
    @GET("api/bins/nearby")
    Call<List<Bin>> getNearbyBins(@Query("latitude") double lat, @Query("longitude") double lng);

    // Raw response for nearby bins to handle server format
    @GET("api/bins/nearby")
    Call<ResponseBody> getNearbyBinsRaw(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("radius") int radius
    );

    // New endpoint for Da Nang nearby bins
    @GET("api/app/bins/nearby/danang")
    Call<ResponseBody> getNearbyBinsDaNang();

    // API cho chức năng báo thùng đầy/tràn
    @POST("api/app/reports")
    Call<ResponseBody> createReport(@Body ReportRequest request);


    // Trong file ApiService.java

// ... các import và các lời gọi API khác

    // Sửa dòng này
    @Multipart
    @POST("reports/upload") // Hoặc endpoint upload của bạn
    Call<ApiResponse<String>> uploadReportImage(@Part MultipartBody.Part file, @Part("description") RequestBody description);


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

//    @GET("p/?depth=1")
//    Call<List<Province>> getProvinces();
//
//    @GET("p/{provinceCode}?depth=2")
//    Call<ProvinceWithDistrictResponse> getDistrictsWithProvince(@Path("provinceCode") String provinceCode);
//
//    @GET("d/{districtCode}?depth=2") // ví dụ nếu API dùng theo kiểu này
//    Call<WardResponse> getWardsWithDistrict(@Path("districtCode") String districtCode);
//

    @POST("api/accounts/forgot-password")
    Call<ResponseBody> forgotPassword(@Query("email") String email);

    @POST("api/accounts/reset-password")
    Call<ResponseBody> resetPassword(@Query("email") String email, @Query("newPassword") String newPassword);

    @PUT("api/tasks/{id}/status")
    Call<Void> updateTaskStatus(@Path("id") String batchId, @Query("status") String status);


    @GET("api/notifications/received/{receiverId}")
    Call<List<Notification>> getReceivedNotifications(@Path("receiverId") String receiverId);

    @PUT("api/notifications/{id}/read")
    Call<Void> markNotificationAsRead(@Path("id") int id);

    @GET("api/notifications/unread")
    Call<List<Notification>> getUnreadNotifications(@Query("userId") String userId);


}