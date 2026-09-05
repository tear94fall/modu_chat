package com.example.modumessenger.Retrofit;

import com.example.modumessenger.dto.ChatReadCursorDto;
import com.example.modumessenger.dto.ChatRoomDto;
import com.example.modumessenger.dto.ChatRoomUnreadDto;
import com.example.modumessenger.dto.MemberDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RetrofitChatRoomAPI {

    @GET("chat-service/api-public/chat/{memberId}/rooms")
    Call<List<ChatRoomDto>> RequestChatRooms(@Path("memberId") String memberId);

    @GET("chat-service/api-public/chat/{roomId}/room")
    Call<ChatRoomDto> RequestChatRoom(@Path("roomId") String roomId);

    @GET("chat-service/api-public/chat/search/{roomName}")
    Call<List<ChatRoomDto>> RequestSearchChatRooms(@Path("roomName") String roomName);

    @POST("chat-service/api-public/chat/chat/room")
    Call<ChatRoomDto> RequestCreateChatRoom(@Body List<Long> ids);

    @DELETE("chat-service/api-public/chat/{roomId}/member/{userId}")
    Call<ChatRoomDto> RequestExitChatRoom(@Path("roomId") String roomId, @Path("userId") String userId);

    @POST("chat-service/api-public/chat/{roomId}/room")
    Call<ChatRoomDto> RequestUpdateChatRoom(@Path("roomId") String roomId, @Body ChatRoomDto chatRoomDto);

    @POST("chat-service/api-public/chat/{roomId}/member")
    Call<ChatRoomDto> RequestAddMemberChatRoom(@Path("roomId") String roomId, @Body List<String> userIds);

    @GET("chat-service/api-public/chat/unread/{userId}")
    Call<List<ChatRoomUnreadDto>> RequestUnreadCounts(@Path("userId") String userId);

    @POST("chat-service/api-public/chat/read/{roomId}/{userId}")
    Call<Void> RequestUpdateLastRead(@Path("roomId") String roomId, @Path("userId") String userId);

    @GET("chat-service/api-public/chat/read/{roomId}")
    Call<List<ChatReadCursorDto>> RequestReadCursors(@Path("roomId") String roomId);
}