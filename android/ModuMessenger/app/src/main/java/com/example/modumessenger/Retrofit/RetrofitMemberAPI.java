package com.example.modumessenger.Retrofit;

import com.example.modumessenger.dto.AddFriendDto;
import com.example.modumessenger.dto.RenameFriendDto;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.dto.PageResponseDto;
import com.example.modumessenger.dto.UpdateProfileDto;

import java.util.List;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RetrofitMemberAPI {

    @GET("member-service/api-public/member/{email}")
    Call<MemberDto> RequestUserInfo(@Path("email") String email);

    @GET("member-service/api-public/member/member/{id}")
    Call<MemberDto> RequestMemberById(@Path("id") Long id);

    @POST("member-service/api-public/member/{userId}")
    Call<MemberDto> RequestUpdateProfile(@Path("userId") String userId, @Body UpdateProfileDto updateProfileDto);

    /** 친구 목록. sort 는 {@link com.example.modumessenger.Global.FriendSort} 값, 서버가 정렬해 page/size 로 잘라 준다. */
    @GET("member-service/api-public/member/{userId}/friends")
    Call<PageResponseDto<MemberDto>> RequestFriends(@Path("userId") String userId, @Query("sort") String sort, @Query("page") int page, @Query("size") int size);

    @POST("member-service/api-public/member/{userId}/friends")
    Call<MemberDto> RequestAddFriends(@Path("userId") String userId, @Body AddFriendDto addFriendDto);

    /** friend userId → 내가 정한 이름 전체. */
    @GET("member-service/api-public/member/{userId}/friends/names")
    Call<Map<String, String>> RequestFriendNames(@Path("userId") String userId);

    @PUT("member-service/api-public/member/{userId}/friends/{friendMemberId}/name")
    Call<MemberDto> RequestRenameFriend(@Path("userId") String userId, @Path("friendMemberId") Long friendMemberId, @Body RenameFriendDto body);

    @GET("member-service/api-public/member/friends/{email}")
    Call<List<MemberDto>> RequestFriend(@Path("email") String email);

    @DELETE("member-service/api-public/member/profile/{userId}")
    Call<MemberDto> RequestDeleteProfileImage(@Path("userId") String userId, @Body String image);
}