package com.example.modumessenger.Retrofit;

import com.example.modumessenger.dto.ChatDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface RetrofitChatAPI {

    @PUT("chat-service/chat/{userId}/token")
    Call<String> RequestFcmToken(@Path("userId") String userId, @Body String fcmToken);

    @GET("chat-service/chat/{roomId}/chats")
    Call<List<ChatDto>> RequestChatHistory(@Path("roomId") String roomId);

    @GET("chat-service/chat/{roomId}/page/{size}")
    Call<List<ChatDto>> RequestChatListSize(@Path("roomId") String roomId, @Path("size") String size);

    @GET("chat-service/chat/{roomId}/{chatId}/{size}")
    Call<List<ChatDto>> RequestPrevChatList(@Path("roomId") String roomId, @Path("chatId") String chatId, @Path("size") String size);

    @GET("chat-service/chat/{roomId}/images/{size}")
    Call<List<ChatDto>> RequestImageChatListSize(@Path("roomId") String roomId, @Path("size") String size);
}
