package com.example.modumessenger.Activity;

import static com.example.modumessenger.Global.DataStoreHelper.*;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;

import com.bumptech.glide.Glide;
import com.example.modumessenger.Adapter.ChatBubble;
import com.example.modumessenger.Adapter.ChatHistoryAdapter;
import com.example.modumessenger.Adapter.ChatRoomMemberAdapter;
import com.example.modumessenger.Global.App;
import com.example.modumessenger.Global.ChatBanner;
import com.example.modumessenger.Global.socket.ConnectionState;
import com.example.modumessenger.Grid.RecentChatImageGridAdapter;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitChatAPI;
import com.example.modumessenger.Retrofit.RetrofitChatRoomAPI;
import com.example.modumessenger.ViewModel.ChatViewModel;
import com.example.modumessenger.ViewModel.ChatViewModelFactory;
import com.example.modumessenger.entity.ChatRoom;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.ChatDto;
import com.example.modumessenger.dto.ChatRoomDto;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity implements ChatSendOthersActivity.ChatSendOthersBottomSheetListener {
    Member member;
    ChatRoom roomInfo;

    ChatViewModel chatViewModel;

    List<Member> chatMemberList;
    List<ChatBubble> chatBubbleList;
    ArrayList<String> recentImageList;
    int pagingSize = 20;

    RecyclerView recyclerView;
    LinearLayoutManager manager;
    ChatHistoryAdapter chatHistoryAdapter;

    ChatSendOthersActivity chatSendOthersActivity;

    TextView inputMsgTextView;
    ImageButton sendMsg, sendOthers;

    View scrollToBottomContainer;
    TextView scrollToBottomBadge;
    /** 점프 버튼이 떠 있는 동안 쌓인, 아직 못 본 상대방 메시지 개수. 버튼을 감출 때 0으로 되돌린다. */
    int unseenJumpCount = 0;

    String roomId;

    ActionBarDrawerToggle actionBarDrawerToggle;

    RetrofitChatAPI retrofitChatAPI;
    RetrofitChatRoomAPI retrofitChatRoomAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        bindingView();
        getData();
        setData();
        setViewModel();
        setButtonClickEvent();
        setScrollEvent();
        settingSideNavBar();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateRoomInfo(roomId);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return false;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chatroom, menu);
        return true;
    }

    private void getData() {
        retrofitChatAPI = RetrofitClient.createChatApiService();
        retrofitChatRoomAPI = RetrofitClient.createChatRoomApiService();

        member = getDataStoreMember();

        roomId = getIntent().getStringExtra("roomId");
        if(roomId != null && !roomId.equals("")) {
            getRoomInfo(roomId);
        }
    }

    private void setData() {
        chatBubbleList = new ArrayList<>();
        chatMemberList = new ArrayList<>();

        chatSendOthersActivity = new ChatSendOthersActivity();
    }

    private void bindingView() {
        recyclerView = findViewById(R.id.chat_history_recycler_view);
        manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(manager);

        sendMsg = findViewById(R.id.send_message_button);
        sendOthers = findViewById(R.id.send_others_button);
        inputMsgTextView = findViewById(R.id.chat_message_edit_text);
        inputMsgTextView.setEnabled(true);

        scrollToBottomContainer = findViewById(R.id.scroll_to_bottom_container);
        scrollToBottomBadge = findViewById(R.id.scroll_to_bottom_badge);
        scrollToBottomContainer.setOnClickListener(v -> {
            recyclerView.scrollToPosition(chatHistoryAdapter.getItemCount() - 1);
            hideJumpToBottomButton();
        });
    }

    /** 새 메시지 배지와 함께 맨 아래로 이동 버튼을 띄운다. */
    private void showJumpToBottomButton(int unseenCount) {
        scrollToBottomContainer.setVisibility(View.VISIBLE);
        if (unseenCount > 0) {
            scrollToBottomBadge.setVisibility(View.VISIBLE);
            scrollToBottomBadge.setText(String.valueOf(unseenCount));
        } else {
            scrollToBottomBadge.setVisibility(View.GONE);
        }
    }

    /** 버튼을 탭했거나 스크롤로 바닥에 닿았을 때 감추고 카운트를 초기화한다. */
    private void hideJumpToBottomButton() {
        unseenJumpCount = 0;
        scrollToBottomContainer.setVisibility(View.GONE);
        scrollToBottomBadge.setVisibility(View.GONE);
    }

    private void setViewModel() {
        chatViewModel = new ViewModelProvider(
                this,
                new ChatViewModelFactory(App.getChatRepository(), roomId, member.getUserId())
        ).get(ChatViewModel.class);

        chatHistoryAdapter = new ChatHistoryAdapter(chatBubbleList, chatMemberList);
        chatHistoryAdapter.setFailedChatActionListener(new ChatHistoryAdapter.FailedChatActionListener() {
            @Override
            public void onResend(ChatBubble chatBubble) {
                confirmFailedAction("메시지를 다시 보낼까요?", "재전송", () -> chatViewModel.resendFailed(chatBubble));
            }

            @Override
            public void onDelete(ChatBubble chatBubble) {
                confirmFailedAction("이 메시지를 삭제할까요?", "삭제", () -> chatViewModel.deleteFailed(chatBubble));
            }
        });
        recyclerView.setAdapter(chatHistoryAdapter);

        chatViewModel.getChats().observe(this, bubbles -> {
            boolean atBottom = !recyclerView.canScrollVertically(1);
            boolean forceScrollToBottom = chatViewModel.consumePendingScrollToBottom();

            int prevCount = chatHistoryAdapter.getItemCount();
            Long prevFirstId = prevCount == 0 ? null : chatBubbleList.get(0).getId();
            int firstVisible = manager.findFirstVisibleItemPosition();
            View firstVisibleView = manager.findViewByPosition(firstVisible);
            int offset = firstVisibleView == null ? 0 : firstVisibleView.getTop();

            chatHistoryAdapter.setChatList(bubbles);

            int newCount = chatHistoryAdapter.getItemCount();
            Long newFirstId = newCount == 0 ? null : chatBubbleList.get(0).getId();
            int added = newCount - prevCount;
            boolean prepended = prevFirstId != null && newFirstId != null
                    && !prevFirstId.equals(newFirstId);

            if (forceScrollToBottom || (atBottom && newCount > 0)) {
                // setChatList 가 이미 notifyDataSetChanged 를 걸어둔 뒤라, 여기서 부르는
                // scrollToPosition 은 다음 레이아웃 패스에서 반영된다. (클릭 핸들러에서
                // 곧바로 불렀다면 아직 갱신 전이라 아무 일도 안 일어났을 것이다.)
                recyclerView.scrollToPosition(newCount - 1);
                hideJumpToBottomButton();
            } else if (prepended && added > 0 && firstVisible >= 0) {
                // 앞쪽에 과거 채팅이 끼어들면 인덱스가 밀린다. 보고 있던 항목과
                // 픽셀 오프셋으로 되돌려 읽던 위치를 유지한다.
                manager.scrollToPositionWithOffset(firstVisible + added, offset);
            } else if (!prepended && added > 0 && newCount > 0) {
                // 과거를 읽는 중에 새 메시지가 뒤에 붙었다. 내가 보낸 메시지는 위에서
                // forceScrollToBottom 으로 이미 처리됐으니 여기 남는 건 상대방 메시지뿐이다.
                // 화면을 억지로 끌고 가지 않고 버튼으로만 알린다.
                ChatBubble lastBubble = chatBubbleList.get(newCount - 1);
                if (ChatViewModel.shouldShowJumpToBottom(atBottom, lastBubble.getSender(), member.getUserId())) {
                    unseenJumpCount += added;
                    showJumpToBottomButton(unseenJumpCount);
                }
            }
        });

        chatViewModel.getBanner().observe(this, event -> ChatBanner.show(this, event));

        // 끊겨 있어도 전송 버튼은 활성 상태로 둔다. 보내면 send() 가 false 를 돌려주고
        // 실패 말풍선(재전송/삭제)이 떠 사용자가 처리할 수 있다 — 버튼을 막는 것보다 낫다.
        chatViewModel.getConnectionState().observe(this, state -> { });

        chatViewModel.loadInitial(pagingSize);
    }

    /** 재전송/삭제 전에 한 번 더 확인한다. */
    private void confirmFailedAction(String message, String positiveText, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(positiveText, (dialog, which) -> onConfirm.run())
                .setNegativeButton("취소", null)
                .show();
    }

    private void setButtonClickEvent() {
        sendMsg.setOnClickListener(v -> {
            String msg = inputMsgTextView.getText().toString();
            if (msg.length() == 0) return;

            // 성공이든 실패든 메시지는 말풍선으로 들어간다(실패 시 재전송/삭제 가능한
            // 실패 말풍선). 입력창은 항상 비워 같은 글자가 칸과 말풍선에 겹치지 않게 한다.
            // 바닥으로의 스크롤은 여기서 곧바로 하지 않는다 — 어댑터가 아직 갱신되기
            // 전이라 스크롤할 대상이 없다. 대신 getChats() 관찰 콜백이 갱신을 반영한
            // 직후에 스크롤한다 (chatViewModel.consumePendingScrollToBottom() 참고).
            chatViewModel.sendText(msg);
            inputMsgTextView.setText(null);
        });

        sendOthers.setOnClickListener(v -> {
            chatSendOthersActivity.show(getSupportFragmentManager(), chatSendOthersActivity.getTag());
        });
    }

    private void setScrollEvent() {
        recyclerView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if(!recyclerView.canScrollVertically(-1)) {
                    ChatBubble oldest = chatBubbleList.get(0);
                    if (chatBubbleList.size() >= pagingSize) {
                        chatViewModel.loadPrev(oldest.getId().toString(), pagingSize);
                    }
                }

                // 탭이 아니라 직접 스크롤로 바닥에 닿아도 점프 버튼은 감춘다.
                if (!recyclerView.canScrollVertically(1)) {
                    hideJumpToBottomButton();
                }
            }
        });
    }

    public void settingSideNavBar() {
        View headerView = findViewById(R.id.nav_header);
        Toolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white, getTheme()));
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24);

        DrawerLayout drawLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        ConstraintLayout recentImageView = navigationView.findViewById(R.id.recentImageConstraintLayout);

        recentImageView.setOnClickListener(v -> {
            if(recentImageList.size() != 0) {
                Intent intent = new Intent(v.getContext(), ChatImageActivity.class);
                intent.putStringArrayListExtra("chatImageList", recentImageList);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                v.getContext().startActivity(intent);
            } else {
                Toast.makeText(this.getApplicationContext(),"채팅방에 전송된 사진이 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        ConstraintLayout chatRoomMemberView = navigationView.findViewById(R.id.chatRoomMemberConstraintLayout);

        chatRoomMemberView.setOnClickListener(v -> {
            // need to implementation
        });

        Button ExitButton = navigationView.findViewById(R.id.nav_exit_button);
        ExitButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog);

            AlertDialog alertDialog = builder.setMessage("채팅방을 나가시겠습니까?")
                    .setTitle("나가기")
                    .setPositiveButton("아니오", (dialog, which) -> Toast.makeText(getApplicationContext(), "취소", Toast.LENGTH_LONG).show())
                    .setNeutralButton("예", (dialog, which) -> {
                        exitChatRoom(roomId, member.getUserId());
                        Toast.makeText(getApplicationContext(), "채팅방에서 나갑니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setCancelable(false)
                    .create();

            alertDialog.getWindow().setGravity(Gravity.CENTER);
            alertDialog.show();
        });

        Button InviteButton = navigationView.findViewById(R.id.nav_invite_button);
        InviteButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), InviteActivity.class);

            ArrayList<String> currentMembers = new ArrayList<>();
            chatMemberList.forEach(m -> currentMembers.add(m.getUserId()));

            intent.putExtra("roomId", roomId);
            intent.putStringArrayListExtra("currentMember", currentMembers);

            startActivity(intent);
        });

        ImageView chatRoomEditImage = headerView.findViewById(R.id.chat_room_info_setting);
        chatRoomEditImage.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ChatRoomEdit.class);
            intent.putExtra("roomId", roomId);
            startActivity(intent);

            Toast.makeText(getApplicationContext(), "채팅방 설정으로 이동", Toast.LENGTH_SHORT).show();
        });

        actionBarDrawerToggle = new ActionBarDrawerToggle(
                ChatActivity.this,
                drawLayout,
                R.string.open,
                R.string.closed
        ) {
            @Override
            public boolean onOptionsItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == android.R.id.home) {
                    finish();
                } else if (id == R.id.chat_search_button) {
                    Toast.makeText(getApplicationContext(), "채팅 검색", Toast.LENGTH_SHORT).show();
                } else if(id == R.id.chat_room_info_button) {
                    if (drawLayout.isDrawerOpen(GravityCompat.END)) {
                        drawLayout.closeDrawer(GravityCompat.END);
                    } else {
                        drawLayout.openDrawer(GravityCompat.END);
                    }
                }

                return false;
            }
        };

        drawLayout.addDrawerListener(actionBarDrawerToggle);

        navigationView.setNavigationItemSelectedListener(item -> {
            return true;
        });
    }

    public void setNavInfo(ChatRoom chatRoom) {
        View headerView = findViewById(R.id.nav_header);

        String count = roomInfo.getMembers().size() + " 명";
        ((TextView) headerView.findViewById(R.id.menu_header_name)).setText(roomInfo.getRoomName());
        ((TextView) headerView.findViewById(R.id.chat_room_member_count)).setText(count);

        Glide.with(this)
                .load(chatRoom.getRoomImage().equals("") ? R.drawable.basic_profile_image : chatRoom.getRoomImage())
                .error(Glide.with(this)
                        .load(R.drawable.basic_profile_image)
                        .into((ImageView) headerView.findViewById(R.id.chat_room_profile_image)))
                .into((ImageView) headerView.findViewById(R.id.chat_room_profile_image));


        // set recent chat image
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        getImageChatList(navigationView, roomInfo, pagingSize);
    }

    public void setNavMember() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        // set chat member
        RecyclerView chatRecyclerView;
        RecyclerView.LayoutManager chatLayoutManager;

        chatRecyclerView = navigationView.findViewById(R.id.chat_room_member_recycler_view);
        chatRecyclerView.setHasFixedSize(true);

        chatLayoutManager = new LinearLayoutManager(this);
        chatRecyclerView.setLayoutManager(chatLayoutManager);

        chatRecyclerView.setAdapter(new ChatRoomMemberAdapter(chatMemberList));
    }

    public void setChatRoomName(List<Member> chatRoomMembers, String chatRoomName) {
        String roomName = "새로운 채팅방";

        if(!chatRoomName.equals("") && !chatRoomName.equals(roomName)) {
            setTitle(chatRoomName);
            return;
        }

        List<String> userIdList = chatRoomMembers.stream()
                .map(Member::getUserId)
                .filter(userId -> !userId.equals(member.getUserId()))
                .collect(Collectors.toList());

        List<String> usernames = chatRoomMembers.stream()
                .map(Member::getUsername)
                .filter(name -> !name.equals(member.getUsername()))
                .collect(Collectors.toList());

        roomName = String.join(", ", usernames);

        switch(usernames.size()) {
            case 0: // self
                setTitle(String.format("나와의 채팅 (%s)", member.getUsername()));
                break;
            case 1: // 1on1
            default: // multi
                setTitle(roomName);
                break;
        }
    }

    @Override
    public void sendImageChat(String filename) {
        if (!chatViewModel.sendImage(filename)) {
            Toast.makeText(getApplicationContext(), "연결 중입니다. 잠시 후 다시 시도해주세요.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void sendOthersFinish() {
        chatSendOthersActivity.dismiss();
    }

    // Retrofit function
    public void getRoomInfo(String roomId) {
        Call<ChatRoomDto> call = retrofitChatRoomAPI.RequestChatRoom(roomId);

        call.enqueue(new Callback<ChatRoomDto>() {
            @Override
            public void onResponse(@NonNull Call<ChatRoomDto> call, @NonNull Response<ChatRoomDto> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                assert response.body() != null;
                ChatRoomDto chatRoomDto = response.body();
                roomInfo = new ChatRoom(chatRoomDto);

                chatMemberList.clear();
                chatRoomDto.getMembers().forEach(m -> chatMemberList.add(new Member(m)));
                if (chatHistoryAdapter != null) {
                    chatHistoryAdapter.setMemberList(chatMemberList);
                }

                setChatRoomName(chatMemberList, roomInfo.getRoomName());
                setNavInfo(roomInfo);
                setNavMember();

                Log.d("채팅방 정보 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<ChatRoomDto> call, @NonNull Throwable t) {
                Log.e("채팅방 정보 가져오기 요청 실패", t.getMessage());
            }
        });
    }

    public void updateRoomInfo(String roomId) {
        Call<ChatRoomDto> call = retrofitChatRoomAPI.RequestChatRoom(roomId);

        call.enqueue(new Callback<ChatRoomDto>() {
            @Override
            public void onResponse(@NonNull Call<ChatRoomDto> call, @NonNull Response<ChatRoomDto> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                assert response.body() != null;
                ChatRoomDto chatRoomDto = response.body();
                roomInfo = new ChatRoom(chatRoomDto);

                chatMemberList.clear();
                chatRoomDto.getMembers().forEach(m -> chatMemberList.add(new Member(m)));
                if (chatHistoryAdapter != null) {
                    chatHistoryAdapter.setMemberList(chatMemberList);
                }

                setChatRoomName(chatMemberList, roomInfo.getRoomName());
                setNavInfo(roomInfo);

                Log.d("채팅방 정보 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<ChatRoomDto> call, @NonNull Throwable t) {
                Log.e("채팅방 정보 가져오기 요청 실패", t.getMessage());
            }
        });
    }

    public void getImageChatList(View view, ChatRoom chatRoom, int size) {
        Call<List<ChatDto>> call = retrofitChatAPI.RequestImageChatListSize(chatRoom.getRoomId(), Integer.toString(size));

        call.enqueue(new Callback<List<ChatDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatDto>> call, @NonNull Response<List<ChatDto>> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        List<ChatDto> imageChatList = response.body();

                        recentImageList = imageChatList
                                .stream()
                                .map(chatDto -> Long.toString(chatDto.getId()))
                                .collect(Collectors.toCollection(ArrayList::new));

                        GridView recent_chat_images = view.findViewById(R.id.chat_room_chat_image_grid_layout);
                        View recent_chat_images_view = view.findViewById(R.id.view2);
                        RecentChatImageGridAdapter recentChatImageGridAdapter = new RecentChatImageGridAdapter(getApplicationContext());
                        recent_chat_images.setAdapter(recentChatImageGridAdapter);

                        recentChatImageGridAdapter.setGridItems(imageChatList);
                        recentChatImageGridAdapter.setRecentImageList(recentImageList);

                        recent_chat_images.setOnItemClickListener((parent, v, position, id) -> {
                            Intent intent = new Intent(v.getContext(), ChatImageActivity.class);
                            ArrayList<String> imageFileList = imageChatList.stream().skip(position).map(ChatDto::getMessage).collect(Collectors.toCollection(ArrayList::new));
                            intent.putStringArrayListExtra("chatImageList", imageFileList);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            v.getContext().startActivity(intent);
                        });

                        if(imageChatList.size() == 0) {
                            recent_chat_images.setVisibility(View.GONE);
                            recent_chat_images_view.setVisibility(View.GONE);
                        }
                    }
                }

                Log.d("채팅 내역 가져 오기 요청 : ", chatRoom.getRoomId());
            }

            @Override
            public void onFailure(@NonNull Call<List<ChatDto>> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void exitChatRoom(String roomId, String userId) {
        Call<ChatRoomDto> call = retrofitChatRoomAPI.RequestExitChatRoom(roomId, userId);

        call.enqueue(new Callback<ChatRoomDto>() {
            @Override
            public void onResponse(@NonNull Call<ChatRoomDto> call, @NonNull Response<ChatRoomDto> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        ChatRoomDto chatRoomDto = response.body();

                        if(chatRoomDto.getRoomId().equals(roomId)) {
                            chatRoomDto.getMembers().forEach(memberDto -> {
                                if(memberDto.getUserId().equals(userId)){
                                    finish();
                                }
                            });
                        }
                    }
                }

                Log.d("채팅방 나가기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<ChatRoomDto> call, @NonNull Throwable t) {
                Log.e("채팅방 나가기 요청 실패", t.getMessage());
            }
        });
    }
}
