package com.example.modumessenger.Retrofit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://192.168.0.3:8080/";

    public static RetrofitMemberAPI getMemberApiService(){return getInstance().create(RetrofitMemberAPI.class);}
    public static RetrofitChatRoomAPI getChatApiService(){ return getInstance().create(RetrofitChatRoomAPI.class); }
    public static RetrofitCommonDataAPI getCommonApiService(){ return getInstance().create(RetrofitCommonDataAPI.class); }

    private static Retrofit getInstance(){
        Gson gson = new GsonBuilder().setLenient().create();
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }
}

