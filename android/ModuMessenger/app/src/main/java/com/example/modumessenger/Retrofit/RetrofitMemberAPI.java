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

    @POST("member/signup")
    Call<SignUpDto> RequestSignup(@Body GoogleLoginRequest googleLoginRequest);

    @POST("login")
    Call<Void> RequestLogin(@Body RequestLoginDto requestLoginDto);

    @POST("member")
    Call<MemberDto> RequestUserInfo(@Body MemberDto MemberDto);

    @POST("member/{userId}")
    Call<MemberDto> RequestUpdate(@Path("userId") String userId, @Body MemberDto memberDto);

    @GET("member/{userId}/friends")
    Call<List<MemberDto>> RequestFriends(@Path("userId") String userId);

    @POST("member/{userId}/friends")
    Call<MemberDto> RequestAddFriends(@Path("userId") String userId, @Body MemberDto memberDto);

    @GET("member/friends/{email}")
    Call<List<MemberDto>> RequestFriend(@Path("email") String email);
}