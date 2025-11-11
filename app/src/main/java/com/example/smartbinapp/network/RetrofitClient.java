package com.example.smartbinapp.network;

import com.example.smartbinapp.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit;
    private static int currentUrlIndex = 0;
    private static final String[] BASE_URLS = {
            BuildConfig.BASE_URL,
            BuildConfig.BASE_URL_FALLBACK1,
            BuildConfig.BASE_URL_FALLBACK2,
            "http://10.12.16.30:8080/SmartBinWeb_war_exploded/",
            "http://10.12.16.30:8080/SmartBinWeb/",
            "http://10.12.16.30:8080/"
    };

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = createRetrofitInstance(BASE_URLS[currentUrlIndex]);
        }
        return retrofit;
    }

    private static Retrofit createRetrofitInstance(String baseUrl) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .callTimeout(120, TimeUnit.SECONDS);

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            clientBuilder.addInterceptor(logging);
        }

        OkHttpClient client = clientBuilder.build();

        // ✅ Custom Gson xử lý Date dạng epoch milliseconds
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> {
                    try {
                        // Trường hợp null
                        if (json == null || json.isJsonNull()) {
                            return null;
                        }

                        // Nếu là số (epoch time)
                        if (json.getAsJsonPrimitive().isNumber()) {
                            long timestamp = json.getAsLong();
                            return new Date(timestamp);
                        }

                        // Nếu là chuỗi, thử parse sang long
                        String value = json.getAsString();
                        try {
                            long timestamp = Long.parseLong(value);
                            return new Date(timestamp);
                        } catch (NumberFormatException e) {
                            // Nếu không phải số thì Gson tự parse theo định dạng ISO
                            return context.deserialize(json, Date.class);
                        }

                    } catch (Exception e) {
                        android.util.Log.e("RetrofitClient", "❌ Lỗi parse Date: " + e.getMessage());
                        return null;
                    }
                })
                .create();

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    public static void resetRetrofitInstance() {
        currentUrlIndex = (currentUrlIndex + 1) % BASE_URLS.length;
        retrofit = null;
    }
}
