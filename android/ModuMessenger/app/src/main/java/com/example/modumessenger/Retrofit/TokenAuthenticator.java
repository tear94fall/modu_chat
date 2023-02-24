package com.example.modumessenger.Retrofit;

import androidx.annotation.Nullable;

import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.dto.TokenResponseDto;
import com.google.gson.GsonBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TokenAuthenticator implements Authenticator {

    private static TokenAuthenticator instance;
    private static final String BASE_URL = "http://192.168.0.3:8000/";

    OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().setLenient().create())).build();

    static synchronized TokenAuthenticator getInstance() {
        if(instance == null) {
            instance = new TokenAuthenticator();
        }

        return instance;
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NotNull Response response) throws IOException {
        if(response.code() == 401) {
            String token = PreferenceManager.getString("refresh-token");

            RetrofitAuthAPI retrofitAuthAPI = retrofit.create(RetrofitAuthAPI.class);

            retrofit2.Response<TokenResponseDto> retrofitResponse = retrofitAuthAPI.reissue(token).execute();

            if(retrofitResponse.isSuccessful()) {
                TokenResponseDto tokenResponseDto = retrofitResponse.body();

                assert tokenResponseDto != null;
                String accessToken = tokenResponseDto.getAccessToken();
                String refreshToken = tokenResponseDto.getRefreshToken();

                PreferenceManager.setString("access-token", "Bearer" + " " + accessToken);
                PreferenceManager.setString("refresh-token", "Bearer" + " " + refreshToken);

                return response.request().newBuilder()
                        .header("Authorization", refreshToken)
                        .build();
            }
        }else if(response.code() == 500) {
            String token = PreferenceManager.getString("refresh-token");

            RetrofitAuthAPI retrofitAuthAPI = retrofit.create(RetrofitAuthAPI.class);

            retrofit2.Response<TokenResponseDto> retrofitResponse = retrofitAuthAPI.reissue(token).execute();

            if(retrofitResponse.isSuccessful()) {
                TokenResponseDto tokenResponseDto = retrofitResponse.body();

                assert tokenResponseDto != null;
                String accessToken = tokenResponseDto.getAccessToken();
                String refreshToken = tokenResponseDto.getRefreshToken();

                PreferenceManager.setString("access-token", "Bearer" + " " + accessToken);
                PreferenceManager.setString("refresh-token", "Bearer" + " " + refreshToken);

                return response.request().newBuilder()
                        .header("Authorization", refreshToken)
                        .build();
            }
        }

        return null;
    }
}
