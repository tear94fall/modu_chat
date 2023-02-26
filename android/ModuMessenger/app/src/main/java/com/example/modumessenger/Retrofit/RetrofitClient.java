package com.example.modumessenger.Retrofit;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://192.168.0.3:8000/";

    private static OkHttpClient.Builder okHttp = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true);

    private static Retrofit.Builder builder =
            new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().setLenient().create()));

    private static Retrofit retrofit = builder.build();

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static <S> S createService(Class<S> serviceClass) {
        AuthenticationInterceptor interceptor = new AuthenticationInterceptor();

        if (!okHttp.interceptors().contains(interceptor)) {
            okHttp.addInterceptor(interceptor);

            builder.client(okHttp.build());
            retrofit = builder.build();
        }

        return retrofit.create(serviceClass);
    }

    public static RetrofitMemberAPI createMemberApiService(){ return createService(RetrofitMemberAPI.class); }
    public static RetrofitChatRoomAPI createChatRoomApiService(){ return createService(RetrofitChatRoomAPI.class); }
    public static RetrofitChatAPI createChatApiService(){ return createService(RetrofitChatAPI.class); }
    public static RetrofitCommonDataAPI createCommonApiService(){ return createService(RetrofitCommonDataAPI.class); }
    public static RetrofitImageAPI createImageApiService(){ return createService(RetrofitImageAPI.class); }
    public static RetrofitAuthAPI createAuthApiService(){ return createService(RetrofitAuthAPI.class); }
}

