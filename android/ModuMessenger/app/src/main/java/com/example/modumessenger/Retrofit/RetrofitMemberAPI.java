package com.example.modumessenger.Retrofit;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface RetrofitMemberAPI {

    @POST("member/signup")
    Call<Member> RequestSignup(@Body Member member);

    @POST("login")
    Call<Void> RequestLogin(@Body Member member);

    @POST("member")
    Call<Member> RequestUserId(@Body Member member);

}