package com.example.modumessenger.Retrofit;

import com.example.modumessenger.dto.TokenResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;

public interface RetrofitAuthAPI {

    @GET("chat-service/auth/reissue")
    Call<TokenResponseDto> reissue(String refreshToken);
}
