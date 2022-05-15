package com.example.modumessenger.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Adapter.ChatBubble;
import com.example.modumessenger.Adapter.ChatHistoryAdapter;
import com.example.modumessenger.Global.ChatWebSocketListener;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.ChatRoom;
import com.example.modumessenger.Retrofit.Member;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.ChatDto;
import com.example.modumessenger.dto.ChatRoomDto;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {
    ChatRoom roomInfo;

    OkHttpClient client;
    WebSocket webSocket;
    WebSocketListener listener;

    List<ChatBubble> chatBubbleList;

    RecyclerView recyclerView;
    LinearLayoutManager manager;
    ChatHistoryAdapter chatHistoryAdapter;

    TextView inputMsgTextView;
    Button sendMsg, sendOthers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.chat_history_recycler_view);
        manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(manager);

        sendMsg = findViewById(R.id.send_message_button);
        sendOthers = findViewById(R.id.send_others_button);
        inputMsgTextView = findViewById(R.id.chat_message_edit_text);
        inputMsgTextView.setEnabled(true);

        chatBubbleList = new ArrayList<>();

        String roomId = getIntent().getStringExtra("roomId");
        if(!roomId.equals("")) {
            getRoomInfo(roomId);
        }

        sendMsg.setOnClickListener(v -> {
            String msg = inputMsgTextView.getText().toString();
            if(msg.length() !=0) {
                ChatBubble chatBubble = new ChatBubble(msg, 2);
                chatHistoryAdapter.addChatMsg(chatBubble);
                chatBubbleList.add(chatBubble);

                // add send at websocket
                webSocket.send(msg);
            }
        });

        sendOthers.setOnClickListener(v -> {
        });

        // init web socket
        client = new OkHttpClient();

        Request request = new Request
                .Builder()
                .url("ws://localhost:8080/modu-chat")
                .build();
        listener = new ChatWebSocketListener();
        webSocket = client.newWebSocket(request, listener);

        client.dispatcher().executorService().shutdown();

        if(roomInfo!=null) {
            getChatList(roomInfo);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        webSocket.close(1000, null);
    }

    public void getRoomInfo(String roomId) {
        Call<ChatRoom> call = RetrofitClient.getChatApiService().RequestChatRoom(roomId);

        call.enqueue(new Callback<ChatRoom>() {
            @Override
            public void onResponse(@NonNull Call<ChatRoom> call, @NonNull Response<ChatRoom> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                assert response.body() != null;
                roomInfo = response.body();

                setTitle(roomInfo.getRoomName());
                getChatList(roomInfo);

                Log.d("채팅방 정보 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<ChatRoom> call, @NonNull Throwable t) {
                Log.e("채팅방 정보 가져오기 요청 실패", t.getMessage());
            }
        });
    }

    public void getChatList(ChatRoom chatRoom) {
        Call<List<ChatDto>> call = RetrofitClient.getChatApiService().RequestChatHistory(chatRoom.getRoomId());

        call.enqueue(new Callback<List<ChatDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatDto>> call, @NonNull Response<List<ChatDto>> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                List<ChatDto> chatHistory = response.body();
                assert chatHistory != null;
                for (ChatDto chatDto : chatHistory) {
                    chatBubbleList.add(new ChatBubble(chatDto));
                }

                chatHistoryAdapter = new ChatHistoryAdapter(chatBubbleList);

                Log.d("채팅 내역 가져오기 요청 : ", chatRoom.getRoomId());
            }

            @Override
            public void onFailure(@NonNull Call<List<ChatDto>> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }
}
