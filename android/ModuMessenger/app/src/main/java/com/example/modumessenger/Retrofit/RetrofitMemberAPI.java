package com.example.modumessenger.Retrofit;

import com.example.modumessenger.dto.MemberDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RetrofitMemberAPI {

    @POST("member/signup")
    Call<Member> RequestSignup(@Body Member member);

    @POST("login")
    Call<Void> RequestLogin(@Body Member member);

    @POST("member")
    Call<Member> RequestUserId(@Body Member member);

    @GET("member/{userId}/friends")
    Call<List<MemberDto>> RequestFriends(@Path("userId") String userId);

    @POST("member/{userId}/friends")
    Call<Member> RequestAddFriends(@Path("userId") String userId, @Body Member member);

    @GET("member/friends/{email}")
    Call<List<MemberDto>> RequestFriend(@Path("email") String email);
}