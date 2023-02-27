package com.example.modumessenger.Activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Adapter.InviteAdapter;
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitChatRoomAPI;
import com.example.modumessenger.Retrofit.RetrofitMemberAPI;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.ChatRoomDto;
import com.example.modumessenger.dto.MemberDto;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InviteActivity extends AppCompatActivity {
    RecyclerView addChatRecyclerView;
    RecyclerView.LayoutManager addChatLayoutManager;
    InviteAdapter inviteAdapter;

    Button inviteButton;

    List<MemberDto> friendsList;

    ArrayList<String> currentMember;
    Member member;
    String roomId;

    RetrofitMemberAPI retrofitMemberAPI;
    RetrofitChatRoomAPI retrofitChatRoomAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite);

        bindingView();
        getData();
        setData();
        setButtonClickEvent();
    }

    private void bindingView() {
        setTitle("친구 초대 하기");

        addChatRecyclerView = (RecyclerView) findViewById(R.id.invite_friend_recycler_view);
        addChatRecyclerView.setHasFixedSize(true);

        addChatLayoutManager = new LinearLayoutManager(this);
        addChatRecyclerView.setLayoutManager(addChatLayoutManager);
        addChatRecyclerView.scrollToPosition(0);

        inviteButton = findViewById(R.id.invite_button);
    }

    private void getData() {
        retrofitMemberAPI = RetrofitClient.createMemberApiService();
        retrofitChatRoomAPI = RetrofitClient.createChatRoomApiService();

        friendsList = new ArrayList<>();
    }

    private void setData() {
        roomId = getIntent().getStringExtra("roomId");
        currentMember = getIntent().getStringArrayListExtra("currentMember");
        member = new Member(PreferenceManager.getString("userId"), PreferenceManager.getString("email"));

        getFriendsList(member);
    }

    private void setButtonClickEvent() {
        inviteButton.setOnClickListener(v -> {
            List<String> inviteMemberList = inviteAdapter.getInviteMemberList();

            if (inviteMemberList.size()!=0) {
                Toast.makeText(getApplicationContext(), "친구를 채팅방에 초대합니다.", Toast.LENGTH_SHORT).show();
                inviteChatRoom(inviteMemberList);
            } else {
                Toast.makeText(getApplicationContext(), "초대할 친구가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Retrofit function
    public void getFriendsList(Member member) {
        Call<List<MemberDto>> call = retrofitMemberAPI.RequestFriends(member.getUserId());

        call.enqueue(new Callback<List<MemberDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<MemberDto>> call, @NonNull Response<List<MemberDto>> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                assert response.body() != null;
                List<MemberDto> allMembers = response.body();

                allMembers.forEach(friends -> {
                    if(!currentMember.contains(friends.getUserId())) {
                        friendsList.add(friends);
                    }
                });

                inviteAdapter = new InviteAdapter(friendsList);
                addChatRecyclerView.setAdapter(inviteAdapter);

                Log.d("친구 리스트 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<List<MemberDto>> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void inviteChatRoom(List<String> userIds) {
        Call<ChatRoomDto> call = retrofitChatRoomAPI.RequestAddMemberChatRoom(roomId, userIds);

        call.enqueue(new Callback<ChatRoomDto>() {
            @Override
            public void onResponse(@NonNull Call<ChatRoomDto> call, @NonNull Response<ChatRoomDto> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code() + ", body : " + response.body());
                    return;
                }

                assert response.body() != null;
                ChatRoomDto chatRoomDto = response.body();

                userIds.forEach(invite -> chatRoomDto.getMembers().forEach(member -> {
                    if(member.getUserId().equals(invite)) {
                        Toast.makeText(getApplicationContext(), invite + "님을 채팅방 초대에 실패하엿습니다. ", Toast.LENGTH_SHORT).show();
                    }
                }));

                finish();

                Log.d("채팅방 초대 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<ChatRoomDto> call, @NonNull Throwable t) {
                Log.e("채팅방 초대 요청 실패", t.getMessage());
            }
        });
    }
}

