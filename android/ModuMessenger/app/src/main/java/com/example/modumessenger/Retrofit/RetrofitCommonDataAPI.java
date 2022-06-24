package com.example.modumessenger.Retrofit;

import com.example.modumessenger.entity.CommonData;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RetrofitCommonDataAPI {

    @GET("common/{key}")
    Call<CommonData> RequestCommonData(@Path("key") String key);
}
