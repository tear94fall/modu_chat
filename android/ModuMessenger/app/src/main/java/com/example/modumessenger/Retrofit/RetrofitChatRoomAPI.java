package com.example.modumessenger.Retrofit;

import com.example.modumessenger.dto.ChatRoomDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RetrofitChatRoomAPI {

    @GET("chat/{userId}/rooms")
    Call<List<ChatRoomDto>> RequestChatRooms(@Path("userId") String userId);
}