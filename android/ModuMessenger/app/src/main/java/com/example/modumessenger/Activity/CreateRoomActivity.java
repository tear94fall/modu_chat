package com.example.modumessenger.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Adapter.CreateRoomAdapter;
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.ChatRoomDto;
import com.example.modumessenger.dto.MemberDto;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateRoomActivity extends AppCompatActivity {
    private static CreateRoomActivity instance;

    RecyclerView addChatRecyclerView;
    RecyclerView.LayoutManager addChatLayoutManager;
    CreateRoomAdapter createRoomAdapter;

    Button inviteButton;

    List<String> addChatList;
    List<MemberDto> friendsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);

        instance = this;
        setTitle("채팅방 만들기");

        addChatRecyclerView = (RecyclerView) findViewById(R.id.create_chatroom_recycler_view);
        addChatRecyclerView.setHasFixedSize(true);

        addChatLayoutManager = new LinearLayoutManager(this);
        addChatRecyclerView.setLayoutManager(addChatLayoutManager);
        addChatRecyclerView.scrollToPosition(0);

        Member member = new Member(PreferenceManager.getString("userId"), PreferenceManager.getString("email"));

        getFriendsList(member);

        addChatList = new ArrayList<>();

        inviteButton = findViewById(R.id.create_chatroom_button);
        inviteButton.setOnClickListener(v -> {
            if (addChatList.size()!=0) {
                Toast.makeText(getApplicationContext(), "채팅방을 생성합니다.", Toast.LENGTH_SHORT).show();
                addChatList.add(member.getUserId());
                createChatRoom(addChatList);
            } else {
                Toast.makeText(getApplicationContext(), "추가할 친구가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static CreateRoomActivity getInstance() {
        return instance;
    }

    public void addUserIdOnAddChatList(String userId) {
        if(!PreferenceManager.getString("userId").equals(userId)) {
            this.addChatList.add(userId);
        }
    }

    public void removeUserIdOnAddChatList(String userId) {
        if(!PreferenceManager.getString("userId").equals(userId)) {
            this.addChatList.remove(userId);
        }
    }

    // Retrofit function
    public void getFriendsList(Member member) {
        Call<List<MemberDto>> call = RetrofitClient.getMemberApiService().RequestFriends(member.getUserId());

        call.enqueue(new Callback<List<MemberDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<MemberDto>> call, @NonNull Response<List<MemberDto>> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                assert response.body() != null;
                friendsList = response.body();
                createRoomAdapter = new CreateRoomAdapter(friendsList);
                addChatRecyclerView.setAdapter(createRoomAdapter);

                Log.d("친구 리스트 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<List<MemberDto>> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void createChatRoom(List<String> userIds) {
        Call<ChatRoomDto> call = RetrofitClient.getChatRoomApiService().RequestCreateChatRoom(userIds);

        call.enqueue(new Callback<ChatRoomDto>() {
            @Override
            public void onResponse(@NonNull Call<ChatRoomDto> call, @NonNull Response<ChatRoomDto> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code() + ", body : " + response.body());
                    return;
                }

                assert response.body() != null;
                ChatRoomDto chatRoomDto = response.body();

                Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                intent.putExtra("roomId", chatRoomDto.getRoomId());
                startActivity(intent);
                finish();

                Log.d("채팅방 생성 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<ChatRoomDto> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }
}

