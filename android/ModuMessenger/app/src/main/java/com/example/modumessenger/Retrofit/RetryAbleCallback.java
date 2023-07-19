package com.example.modumessenger.Retrofit;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.modumessenger.Global.DataStoreHelper;
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.dto.TokenResponseDto;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public abstract class RetryAbleCallback<T> implements Callback<T> {

    private int totalRetries = 3;
    private static final String TAG = RetryAbleCallback.class.getSimpleName();
    private final Call<T> call;
    private int retryCount = 0;

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

    public RetryAbleCallback(Call<T> call, int totalRetries) {
        this.call = call;
        this.totalRetries = totalRetries;
    }

    @Override
    public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
        if (!APIHelper.isCallSuccess(response)) {
            if (retryCount++ < totalRetries) {
                Log.v(TAG, "Retrying API Call -  (" + retryCount + " / " + totalRetries + ")");

                if(response.code() == 500) {
                    reissueToken();
                    retry();
                }
            } else {
                onFinalResponse(call, response);
            }
        } else {
            onFinalResponse(call, response);
        }
    }

    @Override
    public void onFailure(@NonNull Call<T> call, Throwable t) {
        Log.e(TAG, t.getMessage());
        if (retryCount++ < totalRetries) {
            Log.v(TAG, "Retrying API Call -  (" + retryCount + " / " + totalRetries + ")");
            retry();
        } else
            onFinalFailure(call, t);
    }

    public void onFinalResponse(Call<T> call, Response<T> response) {

    }

    public void onFinalFailure(Call<T> call, Throwable t) {
    }

    private void retry() {
        call.clone().enqueue(this);
    }

    private void reissueToken() {
        String accessToken = PreferenceManager.getString("access-token");
        String refreshToken = PreferenceManager.getString("refresh-token");

        RetrofitAuthAPI retrofitAuthAPI = retrofit.create(RetrofitAuthAPI.class);
        Call<TokenResponseDto> call = retrofitAuthAPI.reissue(accessToken, refreshToken);

        call.enqueue(new Callback<TokenResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TokenResponseDto> call, @NonNull Response<TokenResponseDto> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        TokenResponseDto tokenResponseDto = response.body();

                        DataStoreHelper.setDataStoreObject("access-token", "Bearer" + " " + tokenResponseDto.getAccessToken());
                        DataStoreHelper.setDataStoreObject("refresh-token", "Bearer" + " " + tokenResponseDto.getRefreshToken());
                    }
                } else {
                    Log.d("Debug", "body 없음");
                }
            }

            @Override
            public void onFailure(Call<TokenResponseDto> call, Throwable t) {
                t.printStackTrace();
                Log.d("Debug", "onFailure 실행" + t.toString());
            }
        });
    }
}
