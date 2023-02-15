package com.example.modumessenger.Retrofit;

import com.example.modumessenger.dto.TokenResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface RetrofitAuthAPI {

    @GET("chat-service/auth/reissue")
    Call<TokenResponseDto> reissue(String refreshToken);

    @POST("chat-service/auth/logout")
    Call<Void> logout();
}
