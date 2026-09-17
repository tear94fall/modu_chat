package com.example.modumessenger.Retrofit;

import com.example.modumessenger.dto.SsoCodeRequestDto;
import com.example.modumessenger.dto.SsoCodeResponseDto;
import com.example.modumessenger.dto.TokenResponseDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface RetrofitAuthAPI {

    /** 구글 ID 토큰 교환·리프레시. 폼 내용은 {@link com.example.modumessenger.Global.OAuthClient}. */
    @FormUrlEncoded
    @POST("auth-service/oauth2/token")
    Call<TokenResponseDto> token(@FieldMap Map<String, String> form);

    /** 리프레시 토큰 폐기(로그아웃) */
    @FormUrlEncoded
    @POST("auth-service/oauth2/revoke")
    Call<Void> revoke(@Field("token") String token, @Field("client_id") String clientId);

    /** 다른 모두 앱에 넘겨줄 1회용 SSO 코드 */
    @POST("auth-service/api-public/auth/sso-code")
    Call<SsoCodeResponseDto> ssoCode(@Body SsoCodeRequestDto body);
}
