package com.example.modumessenger.Retrofit;

import com.example.modumessenger.dto.GoogleLoginRequest;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.dto.RequestLoginDto;
import com.example.modumessenger.dto.SignUpDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RetrofitMemberAPI {

    @POST("member-service/member/signup")
    Call<SignUpDto> RequestSignup(@Body GoogleLoginRequest googleLoginRequest);

    @GET("member-service/member/{email}")
    Call<MemberDto> RequestUserInfo(@Path("email") String email);

    @POST("member-service/member/{userId}")
    Call<MemberDto> RequestUpdate(@Path("userId") String userId, @Body MemberDto memberDto);

    @GET("member-service/member/{userId}/friends")
    Call<List<MemberDto>> RequestFriends(@Path("userId") String userId);

    @POST("member-service/member/{userId}/friends")
    Call<MemberDto> RequestAddFriends(@Path("userId") String userId, @Body MemberDto memberDto);

    @GET("member-service/member/friends/{email}")
    Call<List<MemberDto>> RequestFriend(@Path("email") String email);
}