package com.example.modumessenger.Retrofit;

import com.example.modumessenger.dto.CreateProfileDto;
import com.example.modumessenger.dto.ProfileDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RetrofitProfileAPI {

    @GET("profile-service/profile/{memberId}")
    Call<List<ProfileDto>> getMemberProfiles(@Path("memberId") Long memberId);

    @POST("profile-service/profile")
    Call<ProfileDto> RequestCreateProfile(@Body CreateProfileDto createProfileDto);
}
