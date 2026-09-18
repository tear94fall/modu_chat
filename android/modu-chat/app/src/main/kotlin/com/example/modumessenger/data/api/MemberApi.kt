package com.example.modumessenger.data.api

import com.example.modumessenger.data.dto.AddFriendDto
import com.example.modumessenger.data.dto.FriendFlagDto
import com.example.modumessenger.data.dto.MemberDto
import com.example.modumessenger.data.dto.PageResponseDto
import com.example.modumessenger.data.dto.RenameFriendDto
import com.example.modumessenger.data.dto.UpdateProfileDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MemberApi {

    @GET("member-service/api-public/member/{email}")
    suspend fun getMemberByEmail(@Path("email") email: String): MemberDto

    @GET("member-service/api-public/member/member/{id}")
    suspend fun getMember(@Path("id") id: Long): MemberDto

    /** 회원 탈퇴(본인). 서버는 204 로 답하고, 남의 userId 면 403 이다. */
    @DELETE("member-service/api-public/member/{userId}")
    suspend fun withdraw(@Path("userId") userId: String)

    @POST("member-service/api-public/member/{userId}")
    suspend fun updateMember(
        @Path("userId") userId: String,
        @Body body: UpdateProfileDto,
    ): MemberDto

    /**
     * `sort` 는 서버 `FriendSort` 문자열, `size` 는 서버가 100 까지만 받는다.
     * `filter` 는 `normal|favorite|hidden|blocked`(서버 기본값 `normal`), 모르는 값이면 400 이다.
     */
    @GET("member-service/api-public/member/{userId}/friends")
    suspend fun getFriends(
        @Path("userId") userId: String,
        @Query("sort") sort: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("filter") filter: String,
    ): PageResponseDto<MemberDto>

    /** 친구 한 명의 상태(즐겨찾기·숨김·차단). 친구가 아니면 404. */
    @GET("member-service/api-public/member/{userId}/friends/{friendMemberId}")
    suspend fun getFriend(
        @Path("userId") userId: String,
        @Path("friendMemberId") friendMemberId: Long,
    ): MemberDto

    /** 차단한 친구면 400 이다(차단 상태에서는 즐겨찾기를 걸 수 없다). */
    @PUT("member-service/api-public/member/{userId}/friends/{friendMemberId}/favorite")
    suspend fun setFavorite(
        @Path("userId") userId: String,
        @Path("friendMemberId") friendMemberId: Long,
        @Body body: FriendFlagDto,
    ): MemberDto

    /** `on` 이면 HIDDEN, 아니면 NORMAL. */
    @PUT("member-service/api-public/member/{userId}/friends/{friendMemberId}/hidden")
    suspend fun setHidden(
        @Path("userId") userId: String,
        @Path("friendMemberId") friendMemberId: Long,
        @Body body: FriendFlagDto,
    ): MemberDto

    /** `on` 이면 BLOCKED(+ 즐겨찾기 해제), 아니면 NORMAL. */
    @PUT("member-service/api-public/member/{userId}/friends/{friendMemberId}/blocked")
    suspend fun setBlocked(
        @Path("userId") userId: String,
        @Path("friendMemberId") friendMemberId: Long,
        @Body body: FriendFlagDto,
    ): MemberDto

    /** 내가 차단한 친구들의 userId. 차단 메시지를 앱에서 거르는 데 쓴다. */
    @GET("member-service/api-public/member/{userId}/friends/blocked-ids")
    suspend fun getBlockedIds(@Path("userId") userId: String): List<String>

    @POST("member-service/api-public/member/{userId}/friends")
    suspend fun addFriend(
        @Path("userId") userId: String,
        @Body body: AddFriendDto,
    ): MemberDto

    /** 친구 userId → 내가 정한 이름. */
    @GET("member-service/api-public/member/{userId}/friends/names")
    suspend fun getFriendNames(@Path("userId") userId: String): Map<String, String>

    @PUT("member-service/api-public/member/{userId}/friends/{friendMemberId}/name")
    suspend fun renameFriend(
        @Path("userId") userId: String,
        @Path("friendMemberId") friendMemberId: Long,
        @Body body: RenameFriendDto,
    ): MemberDto

    @GET("member-service/api-public/member/friends/{email}")
    suspend fun searchByEmail(@Path("email") email: String): List<MemberDto>

    /** 서버가 DELETE 에 본문을 요구한다(서버 계약이라 그대로 둔다). */
    @HTTP(method = "DELETE", path = "member-service/api-public/member/profile/{userId}", hasBody = true)
    suspend fun deleteProfileImage(
        @Path("userId") userId: String,
        @Body image: String,
    ): MemberDto
}
