package com.example.modumessenger.Retrofit;

import com.example.modumessenger.dto.ProfileDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RetrofitProfileAPI {

    @GET("profile-service/api/v1/profile/{memberId}")
    Call<List<ProfileDto>> getMemberProfiles(@Path("memberId") Long memberId);
}
