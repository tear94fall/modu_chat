package com.example.modumessenger.Retrofit;

import retrofit2.Call;
import retrofit2.http.GET;

public interface RetrofitCommonDataAPI {

    @GET("common/version")
    Call<CommonData> RequestAppVersion();
}
