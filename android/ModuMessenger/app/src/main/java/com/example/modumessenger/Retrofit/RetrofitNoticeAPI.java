package com.example.modumessenger.Retrofit;

import com.example.modumessenger.entity.Notice;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RetrofitNoticeAPI {

    @GET("member-service/api-public/notice")
    Call<List<Notice>> RequestNotices();

    @GET("member-service/api-public/notice/{id}")
    Call<Notice> RequestNotice(@Path("id") Long id);
}
