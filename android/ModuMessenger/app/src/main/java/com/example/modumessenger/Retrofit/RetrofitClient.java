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

    private static OkHttpClient.Builder okHttp = new OkHttpClient().newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS);

    private static Retrofit.Builder builder =
            new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().setLenient().create()));

    private static Retrofit retrofit = builder.build();

    // token
    public static RetrofitMemberAPI createMemberApiService(String token){ return createService(RetrofitMemberAPI.class, token); }
    public static RetrofitChatRoomAPI createChatRoomApiService(String token){ return createService(RetrofitChatRoomAPI.class, token); }
    public static RetrofitChatAPI createChatApiService(String token){ return createService(RetrofitChatAPI.class, token); }
    public static RetrofitCommonDataAPI createCommonApiService(String token){ return createService(RetrofitCommonDataAPI.class, token); }
    public static RetrofitImageAPI createImageApiService(String token){ return createService(RetrofitImageAPI.class, token); }
    public static RetrofitAuthAPI createAuthApiService(String token){ return createService(RetrofitAuthAPI.class, token); }

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static <S> S createService(Class<S> serviceClass) {
        return createService(serviceClass, null);
    }

    public static <S> S createService(Class<S> serviceClass, final String authToken) {
        if (!TextUtils.isEmpty(authToken)) {
            AuthenticationInterceptor interceptor = new AuthenticationInterceptor(authToken);
            TokenAuthenticator authenticator = new TokenAuthenticator();

            if (!okHttp.interceptors().contains(interceptor)) {
                okHttp.addInterceptor(interceptor);
                okHttp.authenticator(authenticator);

                builder.client(okHttp.build());
                retrofit = builder.build();
            }
        }

        return retrofit.create(serviceClass);
    }
}

