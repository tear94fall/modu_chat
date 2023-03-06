package com.example.modumessenger.Retrofit;

import com.example.modumessenger.dto.TokenResponseDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface RetrofitAuthAPI {

    @POST("member-service/auth/reissue")
    Call<TokenResponseDto> reissue(@Header("Authorization") String accessToken, @Header("refresh-token") String refreshToken);

    @POST("member-service/auth/logout")
    Call<Void> logout();
}
