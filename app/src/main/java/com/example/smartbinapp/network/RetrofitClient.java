package com.example.smartbinapp.network;

import com.example.smartbinapp.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit;
    private static int currentUrlIndex = 0;
    private static final String[] BASE_URLS = {
            // Preferred from build config
            BuildConfig.BASE_URL,
            BuildConfig.BASE_URL_FALLBACK1,
            BuildConfig.BASE_URL_FALLBACK2,
            // Additional fallbacks for common Tomcat context names
            "http://10.0.2.2:8080/SmartBinWeb_war_exploded/",
            "http://10.0.2.2:8080/SmartBinWeb/",
            "http://10.0.2.2:8080/"
    };

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = createRetrofitInstance(BASE_URLS[currentUrlIndex]);
        }
        return retrofit;
    }

    public static Retrofit getRetrofitInstanceWithFallback() {
        // Try current URL first
        if (retrofit != null) {
            return retrofit;
        }
        
        // Try all URLs
        for (int i = 0; i < BASE_URLS.length; i++) {
            try {
                android.util.Log.d("RetrofitClient", "Trying URL " + (i + 1) + ": " + BASE_URLS[i]);
                retrofit = createRetrofitInstance(BASE_URLS[i]);
                currentUrlIndex = i;
                android.util.Log.d("RetrofitClient", "Successfully connected to: " + BASE_URLS[i]);
                return retrofit;
            } catch (Exception e) {
                android.util.Log.w("RetrofitClient", "Failed to connect to: " + BASE_URLS[i] + " - " + e.getMessage());
                retrofit = null;
            }
        }
        
        // If all fail, return the first one as fallback
        android.util.Log.e("RetrofitClient", "All URLs failed, using primary URL as fallback");
        return createRetrofitInstance(BASE_URLS[0]);
    }

    private static Retrofit createRetrofitInstance(String baseUrl) {
        // Logger - bật để debug API calls
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        // ✅ Gson custom: parse Date từ timestamp (long)
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class,
                        (JsonDeserializer<Date>) (json, typeOfT, context) ->
                                new Date(json.getAsJsonPrimitive().getAsLong()))
                .create();

        // Retrofit
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    public static void resetRetrofitInstance() {
        // Rotate to next base URL to actually try alternatives on next call
        currentUrlIndex = (currentUrlIndex + 1) % BASE_URLS.length;
        android.util.Log.w("RetrofitClient", "Resetting Retrofit. Next base URL index: " + currentUrlIndex + " -> " + BASE_URLS[currentUrlIndex]);
        retrofit = null;
    }
}
