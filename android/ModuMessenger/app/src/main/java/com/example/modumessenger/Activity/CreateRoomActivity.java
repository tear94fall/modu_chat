package com.example.modumessenger.Activity;

import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreMember;

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
import com.example.modumessenger.Global.App;
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

public class CreateRoomActivity extends AppCompatActivity {

    private static final int FRIENDS_PAGE_SIZE = 50;
    private static final int LOAD_MORE_THRESHOLD = 5;

    RecyclerView addChatRecyclerView;
    CreateRoomAdapter createRoomAdapter;

    Button inviteButton;

    List<Long> addChatList;
    List<MemberDto> friendsList;
    FriendsPager friendsPager;

    Member member;

    RetrofitMemberAPI retrofitMemberAPI;
    RetrofitChatRoomAPI retrofitChatRoomAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);

        bindingView();
        getData();
        setData();
        setButtonClickEvent();
    }

    private void bindingView() {
        setTitle("채팅방 만들기");

        addChatRecyclerView = findViewById(R.id.create_chatroom_recycler_view);
        addChatRecyclerView.setHasFixedSize(true);
        addChatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        addChatRecyclerView.scrollToPosition(0);

        friendsList = new ArrayList<>();
        createRoomAdapter = new CreateRoomAdapter(friendsList);
        addChatRecyclerView.setAdapter(createRoomAdapter);
        friendsPager = new FriendsPager(FRIENDS_PAGE_SIZE);
        addChatRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return;
                int lastVisible = ((LinearLayoutManager) rv.getLayoutManager()).findLastVisibleItemPosition();
                if (lastVisible >= createRoomAdapter.getItemCount() - LOAD_MORE_THRESHOLD) {
                    loadNextFriendsPage();
                }
            }
        });

        inviteButton = findViewById(R.id.create_chatroom_button);
    }

    private void getData() {
    }

    private void setData() {
        retrofitMemberAPI = RetrofitClient.createMemberApiService();
        retrofitChatRoomAPI = RetrofitClient.createChatRoomApiService();

        member = getDataStoreMember();
        loadNextFriendsPage();

        addChatList = new ArrayList<>();
    }

    private void setButtonClickEvent() {
        inviteButton.setOnClickListener(v -> {
            if (addChatList.size()!=0) {
                Toast.makeText(getApplicationContext(), "채팅방을 생성합니다.", Toast.LENGTH_SHORT).show();
                addChatList.add(member.getId());
                // 같은 멤버 구성의 방이 있으면 서버가 그 방을 돌려주므로 기존 방으로 이동하게 된다.
                createChatRoom(addChatList);
            } else {
                Toast.makeText(getApplicationContext(), "추가할 친구가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void addUserIdOnAddChatList(Long id) {
        if(!member.getId().equals(id)) {
            this.addChatList.add(id);
        }
    }

    public void removeUserIdOnAddChatList(Long id) {
        if(!member.getId().equals(id)) {
            this.addChatList.remove(id);
        }
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
                createRoomAdapter.addAll(body.getContent());

                Log.d("친구 리스트 가져오기 요청 : ", "page " + body.getPage() + " / " + body.getTotalPages());
            }

            @Override
            public void onFailure(@NonNull Call<PageResponseDto<MemberDto>> call, @NonNull Throwable t) {
                friendsPager.onFailed();
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void createChatRoom(List<Long> ids) {
        Call<ChatRoomDto> call = retrofitChatRoomAPI.RequestCreateChatRoom(ids);

        call.enqueue(new Callback<ChatRoomDto>() {
            @Override
            public void onResponse(@NonNull Call<ChatRoomDto> call, @NonNull Response<ChatRoomDto> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code() + ", body : " + response.body());
                    return;
                }

                assert response.body() != null;
                ChatRoomDto chatRoomDto = response.body();

                // 뒤로 돌아왔을 때 목록이 바로 보이도록 여기서 미리 갱신해 둔다.
                if (App.getChatRepository() != null) {
                    App.getChatRepository().refreshChatRooms();
                }

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

