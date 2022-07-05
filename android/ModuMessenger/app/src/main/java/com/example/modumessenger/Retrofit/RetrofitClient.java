package com.example.modumessenger.Retrofit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://192.168.0.3:8080/";

    public static RetrofitMemberAPI getMemberApiService(){return getInstance().create(RetrofitMemberAPI.class);}
    public static RetrofitChatRoomAPI getChatRoomApiService(){ return getInstance().create(RetrofitChatRoomAPI.class); }
    public static RetrofitChatAPI getChatApiService(){ return getInstance().create(RetrofitChatAPI.class); }
    public static RetrofitCommonDataAPI getCommonApiService(){ return getInstance().create(RetrofitCommonDataAPI.class); }
    public static RetrofitImageAPI getImageApiService(){ return getInstance().create(RetrofitImageAPI.class); }

    public static String getBaseUrl() {
        return BASE_URL;
    }

    private static Retrofit getInstance(){
        Gson gson = new GsonBuilder().setLenient().create();

        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson));

        return builder.build();
    }
}

