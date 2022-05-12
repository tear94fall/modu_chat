package com.example.modumessenger.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Adapter.ChatBubble;
import com.example.modumessenger.Adapter.ChatHistoryAdapter;
import com.example.modumessenger.Global.ChatWebSocketListener;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.ChatRoomDto;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {
    ChatRoomDto chatRoomInfo;

    OkHttpClient client;

    private List<ChatBubble> chatBubbleList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        String roomId = getIntent().getStringExtra("roomId");
        getRoomInfo(roomId);

        setTitle(chatRoomInfo != null ? chatRoomInfo.getRoomName() : "채팅방");

        client = new OkHttpClient();

        Request request = new Request.Builder().url("ws://localhost").build();
        ChatWebSocketListener listener = new ChatWebSocketListener();

        WebSocket ws = client.newWebSocket(request, listener);

        client.dispatcher().executorService().shutdown();

        // dummy test
        this.initDummyData();

        RecyclerView recyclerView = findViewById(R.id.chat_history_recycler_view);

        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(new ChatHistoryAdapter(chatBubbleList));
    }

    public void initDummyData() {
        int LEFT = 1;
        int RIGHT = 2;

        chatBubbleList = new ArrayList<>();

        chatBubbleList.add(new ChatBubble("AAAA", LEFT));
        chatBubbleList.add(new ChatBubble("BBBB", RIGHT));
        chatBubbleList.add(new ChatBubble("CCCC", LEFT));
        chatBubbleList.add(new ChatBubble("DDDD", RIGHT));
        chatBubbleList.add(new ChatBubble("EEEE", LEFT));
        chatBubbleList.add(new ChatBubble("FFFF", RIGHT));
        chatBubbleList.add(new ChatBubble("GGGG", LEFT));
    }

    public void getRoomInfo(String roomId) {
        Call<ChatRoomDto> call = RetrofitClient.getChatApiService().RequestChatRoom(roomId);

        call.enqueue(new Callback<ChatRoomDto>() {
            @Override
            public void onResponse(@NonNull Call<ChatRoomDto> call, @NonNull Response<ChatRoomDto> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                assert response.body() != null;
                chatRoomInfo = response.body();
                Log.d("채팅방 정보 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<ChatRoomDto> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }
}
