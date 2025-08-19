package com.example.smartbinapp.network;

import java.util.List;
import java.util.Map;
import com.example.smartbinapp.model.Account;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
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

    @POST("api/login")
    Call<Account> login(@Body Account account);



    @POST("/TestSmartBin_war/api/accounts/register") // Đường dẫn bạn đã tạo trong backend Spring Boot
    Call<Account> register(@Body Account account);


    @POST("/TestSmartBin_war/api/accounts/verificode")
    Call<ResponseBody> verifyCode(@Body Account request);

//    @GET("p/?depth=1")
//    Call<List<Province>> getProvinces();
//
//    @GET("p/{provinceCode}?depth=2")
//    Call<ProvinceWithDistrictResponse> getDistrictsWithProvince(@Path("provinceCode") String provinceCode);
//
//    @GET("d/{districtCode}?depth=2") // ví dụ nếu API dùng theo kiểu này
//    Call<WardResponse> getWardsWithDistrict(@Path("districtCode") String districtCode);
//
}