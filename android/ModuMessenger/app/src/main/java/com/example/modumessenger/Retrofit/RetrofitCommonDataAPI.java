package com.example.modumessenger.Retrofit;

import com.example.modumessenger.entity.CommonData;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RetrofitCommonDataAPI {

    @GET("common/{key}")
    Call<CommonData> RequestCommonData(@Path("key") String key);

    @GET("commons/{key}")
    Call<List<CommonData>> RequestCommonDataList(@Path("key") String key);
}
