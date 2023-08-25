package com.example.modumessenger.Retrofit;

import com.example.modumessenger.dto.CreateProfileDto;
import com.example.modumessenger.dto.ProfileDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RetrofitProfileAPI {

    @GET("profile-service/profile/{memberId}")
    Call<List<ProfileDto>> RequestMemberProfiles(@Path("memberId") Long memberId);

    @GET("profile-service/profile/{memberId}/{id}")
    Call<ProfileDto> RequestMemberProfile(@Path("memberId") String memberId, @Path("id") String id);

    @POST("profile-service/profile")
    Call<ProfileDto> RequestCreateProfile(@Body CreateProfileDto createProfileDto);

    @DELETE("profile-service/{memberId}/{id}")
    Call<Long> RequestDeleteProfile(@Path("memberId") String memberId, @Path("id") String id);

    @GET("profile-service/total/count/{memberId}")
    Call<Long> RequestTotalProfileCount(@Path("memberId") String memberId);
}
