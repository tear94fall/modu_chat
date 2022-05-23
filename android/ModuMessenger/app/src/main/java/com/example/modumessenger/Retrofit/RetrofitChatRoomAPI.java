package com.example.modumessenger.Retrofit;

import com.example.modumessenger.dto.ChatDto;
import com.example.modumessenger.dto.ChatRoomDto;
import com.example.modumessenger.dto.MemberDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RetrofitChatRoomAPI {

    @GET("chat/{userId}/rooms")
    Call<List<ChatRoom>> RequestChatRooms(@Path("userId") String userId);

    @GET("chat/{roomId}/room")
    Call<ChatRoom> RequestChatRoom(@Path("roomId") String roomId);

    @POST("chat/chat/room")
    Call<ChatRoom> RequestCreateChatRoom(@Body List<String> userIds);

    @GET("chat/{roomId}/chats")
    Call<List<ChatDto>> RequestChatHistory(@Path("roomId") String roomId);

    @GET("chat/{roomId}/members")
    Call<List<MemberDto>> RequestChatRoomMembers(@Path("roomId") String roomId);
}