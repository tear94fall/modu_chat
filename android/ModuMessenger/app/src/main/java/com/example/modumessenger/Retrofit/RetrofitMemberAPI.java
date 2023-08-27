package com.example.modumessenger.Retrofit;

import com.example.modumessenger.dto.GoogleLoginRequest;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.dto.RequestLoginDto;
import com.example.modumessenger.dto.SignUpDto;
import com.example.modumessenger.dto.UpdateProfileDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RetrofitMemberAPI {

    @POST("member-service/member/signup")
    Call<SignUpDto> RequestSignup(@Body GoogleLoginRequest googleLoginRequest);

    @GET("member-service/member/{email}")
    Call<MemberDto> RequestUserInfo(@Path("email") String email);

    @GET("member-service/member/member/{id}")
    Call<MemberDto> RequestMemberById(@Path("id") Long id);

    @POST("member-service/member/{userId}")
    Call<MemberDto> RequestUpdateProfile(@Path("userId") String userId, @Body UpdateProfileDto updateProfileDto);

    @GET("member-service/member/{userId}/friends")
    Call<List<MemberDto>> RequestFriends(@Path("userId") String userId);

    @POST("member-service/member/{userId}/friends")
    Call<MemberDto> RequestAddFriends(@Path("userId") String userId, @Body String email);

    @GET("member-service/member/friends/{email}")
    Call<List<MemberDto>> RequestFriend(@Path("email") String email);

    @DELETE("member-service/member/profile/{userId}")
    Call<MemberDto> RequestDeleteProfileImage(@Path("userId") String userId, @Body String image);
}