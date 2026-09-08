package com.example.modumessenger.Activity;

import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreMember;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Adapter.InviteAdapter;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitChatRoomAPI;
import com.example.modumessenger.Retrofit.RetrofitMemberAPI;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.ChatRoomDto;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.dto.PageResponseDto;
import com.example.modumessenger.Global.FriendSort;
import com.example.modumessenger.Global.FriendsPager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InviteActivity extends AppCompatActivity {

    private static final int FRIENDS_PAGE_SIZE = 50;
    private static final int LOAD_MORE_THRESHOLD = 5;

    RecyclerView addChatRecyclerView;
    RecyclerView.LayoutManager addChatLayoutManager;
    InviteAdapter inviteAdapter;

    Button inviteButton;

    List<MemberDto> friendsList;
    FriendsPager friendsPager;

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

        friendsList = new ArrayList<>();
        inviteAdapter = new InviteAdapter(friendsList);
        addChatRecyclerView.setAdapter(inviteAdapter);
        friendsPager = new FriendsPager(FRIENDS_PAGE_SIZE);
        addChatRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return;
                int lastVisible = ((LinearLayoutManager) addChatLayoutManager).findLastVisibleItemPosition();
                if (lastVisible >= inviteAdapter.getItemCount() - LOAD_MORE_THRESHOLD) {
                    loadNextFriendsPage();
                }
            }
        });

        inviteButton = findViewById(R.id.invite_button);
    }

    private void getData() {
        retrofitMemberAPI = RetrofitClient.createMemberApiService();
        retrofitChatRoomAPI = RetrofitClient.createChatRoomApiService();
    }

    private void setData() {
        roomId = getIntent().getStringExtra("roomId");
        currentMember = getIntent().getStringArrayListExtra("currentMember");
        member = getDataStoreMember();

        loadNextFriendsPage();
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
    private void loadNextFriendsPage() {
        if (!friendsPager.canLoad()) return;

        int page = friendsPager.beginLoad();
        Call<PageResponseDto<MemberDto>> call = retrofitMemberAPI.RequestFriends(member.getUserId(), FriendSort.DEFAULT, page, friendsPager.getPageSize());

        call.enqueue(new Callback<PageResponseDto<MemberDto>>() {
            @Override
            public void onResponse(@NonNull Call<PageResponseDto<MemberDto>> call, @NonNull Response<PageResponseDto<MemberDto>> response) {
                if(!response.isSuccessful() || response.body() == null){
                    friendsPager.onFailed();
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                PageResponseDto<MemberDto> body = response.body();
                friendsPager.onLoaded(body);

                // 이미 방에 있는 사람은 페이지마다 걸러 낸다.
                List<MemberDto> invitable = new ArrayList<>();
                body.getContent().forEach(friend -> {
                    if(!currentMember.contains(friend.getUserId())) {
                        invitable.add(friend);
                    }
                });
                inviteAdapter.addAll(invitable);

                // 이 페이지가 전부 걸러졌고 다음 페이지가 있으면 화면이 비어 스크롤이 안 되므로 바로 이어 읽는다.
                if (invitable.isEmpty() && friendsPager.canLoad()) {
                    loadNextFriendsPage();
                }

                Log.d("친구 리스트 가져오기 요청 : ", "page " + body.getPage() + " / " + body.getTotalPages());
            }

            @Override
            public void onFailure(@NonNull Call<PageResponseDto<MemberDto>> call, @NonNull Throwable t) {
                friendsPager.onFailed();
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

