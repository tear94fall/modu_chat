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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;

import com.bumptech.glide.Glide;
import com.example.modumessenger.Adapter.ChatBubble;
import com.example.modumessenger.Adapter.ChatHistoryAdapter;
import com.example.modumessenger.Adapter.ChatRoomMemberAdapter;
import com.example.modumessenger.Global.ActivityStack;
import com.example.modumessenger.Global.DataStoreHelper;
import com.example.modumessenger.Grid.RecentChatImageGridAdapter;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitChatAPI;
import com.example.modumessenger.Retrofit.RetrofitChatRoomAPI;
import com.example.modumessenger.entity.ChatRoom;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.ChatDto;
import com.example.modumessenger.dto.ChatRoomDto;
import com.example.modumessenger.dto.ChatType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.navigation.NavigationView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity implements ChatSendOthersActivity.ChatSendOthersBottomSheetListener {
    Member member;
    ChatRoom roomInfo;

    OkHttpClient client;
    WebSocket webSocket;
    WebSocketListener listener;

    ObjectMapper objectMapper;

    List<Member> chatMemberList;
    List<ChatBubble> chatBubbleList;
    ArrayList<String> recentImageList;
    int pagingSize = 20;

    RecyclerView recyclerView;
    LinearLayoutManager manager;
    ChatHistoryAdapter chatHistoryAdapter;

    ChatSendOthersActivity chatSendOthersActivity;

    TextView inputMsgTextView;
    Button sendMsg, sendOthers;

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
        setEventBus(true);
        setButtonClickEvent();
        setScrollEvent();
        settingSideNavBar();

        ActivityStack.getInstance().regOnCreateState(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateRoomInfo(roomId);
        ActivityStack.getInstance().regOnResumeState(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        ActivityStack.getInstance().regOnPauseState(this);
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
        setEventBus(false);
        ActivityStack.getInstance().regOnDestroyState(this);
    }

    @Override
    public void finish() {
        super.finish();
        webSocket.close(1000, null);
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
        objectMapper = new ObjectMapper();
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
    }

    private void setEventBus(boolean flag) {
        if (flag) {
            EventBus.getDefault().register(this);
        } else {
            EventBus.getDefault().unregister(this);
        }
    }

    private void setButtonClickEvent() {
        sendMsg.setOnClickListener(v -> {
            String msg = inputMsgTextView.getText().toString();
            if(msg.length() !=0) {
                ChatDto chatDto = new ChatDto();
                chatDto.setRoomId(roomId);
                chatDto.setMessage(msg);
                chatDto.setSender(member.getUserId());
                chatDto.setChatTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                chatDto.setChatType(ChatType.CHAT_TYPE_TEXT);

                String message = null;
                try {
                    message = objectMapper.writeValueAsString(chatDto);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                System.out.println(message);

                if(message!=null){
                    webSocket.send(message);
                    recyclerView.scrollToPosition(chatHistoryAdapter.getItemCount() - 1);

                    inputMsgTextView.setText(null);
                } else {
                    Toast.makeText(getApplicationContext(), "메세지 전송에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                }
            }
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
                    ChatBubble lastChat = chatBubbleList.get(0);
                    if(chatBubbleList.size() >= pagingSize) {
                        getPrevChatList(lastChat.getRoomId(), lastChat.getId().toString(), pagingSize);
                    }
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

        DrawerLayout drawLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        ConstraintLayout recentImageView = navigationView.findViewById(R.id.recentImageConstraintLayout);

        recentImageView.setOnClickListener(v -> {
            if(recentImageList.size() != 0) {
                Intent intent = new Intent(v.getContext(), ProfileImageActivity.class);
                intent.putStringArrayListExtra("imageFileList", recentImageList);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                v.getContext().startActivity(intent);
            } else {
                Toast.makeText(this.getApplicationContext(),"채팅방에 전송된 사진이 없습니다.", Toast.LENGTH_SHORT).show();
            }
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent messageEvent) {
        Log.e("event bus call", "Chat Event " + messageEvent.getChatBubble().getChatMsg());
        this.chatBubbleList.add(messageEvent.getChatBubble());
        recyclerView.scrollToPosition(chatHistoryAdapter.getItemCount() - 1);
    }

    @Override
    public void sendImageChat(String filename) {
        ChatDto chatDto = new ChatDto();
        chatDto.setRoomId(roomId);
        chatDto.setMessage(filename);
        chatDto.setSender(member.getUserId());
        chatDto.setChatTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        chatDto.setChatType(ChatType.CHAT_TYPE_IMAGE);

        String message = null;
        try {
            message = objectMapper.writeValueAsString(chatDto);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        if(message!=null){
            webSocket.send(message);
            recyclerView.scrollToPosition(chatHistoryAdapter.getItemCount() - 1);
        } else {
            Toast.makeText(getApplicationContext(), "메세지 전송에 실패하였습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    public String getRoomId() {
        return roomId;
    }

    @Override
    public void sendOthersFinish() {
        chatSendOthersActivity.dismiss();
    }

    // event bus class
    public static class MessageEvent {
        private final ChatBubble chatBubble;

        public MessageEvent(ChatBubble chatBubble) {
            this.chatBubble = chatBubble;
        }

        public ChatBubble getChatBubble() {
            return this.chatBubble;
        }
    }

    public static class ChatWebSocketListener extends okhttp3.WebSocketListener {

        private static final int NORMAL_CLOSURE_STATUS = 1000;

        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull okhttp3.Response response) {
            Log.d("onOpen", "WebSocket connection success");
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            Log.d("onMessage", "WebSocket receive text message");
            System.out.println(text);

            try {
                JSONObject jsonObject = new JSONObject(text);

                String roomId = (String) jsonObject.get("roomId");
                String message = (String) jsonObject.get("message");
                String chatTime = (String) jsonObject.get("chatTime");
                String sender = (String) jsonObject.get("sender");
                int chatType = (int) jsonObject.get("chatType");

                ChatBubble chatBubble = new ChatBubble(roomId, message, chatTime, sender, chatType);

                EventBus.getDefault().post(new MessageEvent(chatBubble));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
            Log.d("onMessage", "WebSocket receive binary message");
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            Log.d("onClosed", "WebSocket connection closed. code : " + code + " message : " + reason);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, @NonNull String reason) {
            Log.d("onClosing", "WebSocket connection closing. code : " + code + " message : " + reason);
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            webSocket.cancel();
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, Throwable t, okhttp3.Response response) {
            Log.d("onClosing", "WebSocket connection fail " + t.getMessage());
            t.printStackTrace();
        }
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

                setChatRoomName(chatMemberList, roomInfo.getRoomName());
                setNavInfo(roomInfo);

                Log.d("채팅방 정보 가져오기 요청 : ", response.body().toString());

                // init web socket
                client = new OkHttpClient();

                Request request = new Request
                        .Builder()
                        .url("ws://192.168.0.3:8000/ws-service/modu-chat/" + roomId)
                        .addHeader("userId", member.getUserId())
                        .addHeader("Authorization", DataStoreHelper.getDataStoreStr("access-token"))
                        .build();
                listener = new ChatWebSocketListener();
                webSocket = client.newWebSocket(request, listener);

                client.dispatcher().executorService().shutdown();

                if(roomInfo!=null) {
                    getChatList(roomInfo, pagingSize);
                }
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

    public void getChatList(ChatRoom chatRoom, int size) {
        Call<List<ChatDto>> call = retrofitChatAPI.RequestChatListSize(chatRoom.getRoomId(), Integer.toString(size));

        call.enqueue(new Callback<List<ChatDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatDto>> call, @NonNull Response<List<ChatDto>> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                List<ChatDto> chatHistory = response.body();
                assert chatHistory != null;
                chatHistory.forEach(c-> chatBubbleList.add(new ChatBubble(c)));

                chatHistoryAdapter = new ChatHistoryAdapter(chatBubbleList, chatMemberList);
                recyclerView.setAdapter(chatHistoryAdapter);
                recyclerView.scrollToPosition(chatHistoryAdapter.getItemCount() - 1);

                setNavMember();

                Log.d("채팅 내역 가져오기 요청 : ", chatRoom.getRoomId());
            }

            @Override
            public void onFailure(@NonNull Call<List<ChatDto>> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void getPrevChatList(String roomId, String chatId, int size) {
        Call<List<ChatDto>> call = retrofitChatAPI.RequestPrevChatList(roomId, chatId, Integer.toString(size));

        call.enqueue(new Callback<List<ChatDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatDto>> call, @NonNull Response<List<ChatDto>> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                List<ChatDto> chatDtoList = response.body();
                assert chatDtoList != null;
                List<ChatBubble> prevChatList = chatDtoList.stream().map(ChatBubble::new).collect(Collectors.toList());
                chatHistoryAdapter.addChatMsgFront(prevChatList);

                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int lastCompletelyVisibleItemPosition = linearLayoutManager != null ? linearLayoutManager.findLastCompletelyVisibleItemPosition() : prevChatList.size();
                recyclerView.scrollToPosition(prevChatList.size() + lastCompletelyVisibleItemPosition);

                Log.d("채팅 내역 가져오기 요청 : ", roomId);
            }

            @Override
            public void onFailure(@NonNull Call<List<ChatDto>> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void getImageChatList(View view, ChatRoom chatRoom, int size) {
        Call<List<ChatDto>> call = retrofitChatAPI.RequestImageChatListSize(chatRoom.getRoomId(), Integer.toString(size));

        call.enqueue(new Callback<List<ChatDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatDto>> call, @NonNull Response<List<ChatDto>> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                List<ChatDto> imageChatList = response.body();
                assert imageChatList != null;

                recentImageList = imageChatList.stream().map(ChatDto::getMessage).collect(Collectors.toCollection(ArrayList::new));

                GridView recent_chat_images = view.findViewById(R.id.chat_room_chat_image_grid_layout);
                View recent_chat_images_view = view.findViewById(R.id.view2);
                RecentChatImageGridAdapter recentChatImageGridAdapter = new RecentChatImageGridAdapter(getApplicationContext());
                recent_chat_images.setAdapter(recentChatImageGridAdapter);

                recentChatImageGridAdapter.setGridItems(imageChatList);

                recent_chat_images.setOnItemClickListener((parent, v, position, id) -> {
                    Intent intent = new Intent(v.getContext(), ProfileImageActivity.class);
                    ArrayList<String> imageFileList = imageChatList.stream().skip(position).map(ChatDto::getMessage).collect(Collectors.toCollection(ArrayList::new));
                    intent.putStringArrayListExtra("imageFileList", imageFileList);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    v.getContext().startActivity(intent);
                });

                if(imageChatList.size() == 0) {
                    recent_chat_images.setVisibility(View.GONE);
                    recent_chat_images_view.setVisibility(View.GONE);
                }

                Log.d("채팅 내역 가져오기 요청 : ", chatRoom.getRoomId());
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
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                assert response.body() != null;
                ChatRoomDto chatRoomDto = response.body();

                if(chatRoomDto.getRoomId().equals(roomId)) {
                    chatRoomDto.getMembers().forEach(memberDto -> {
                        if(memberDto.getUserId().equals(userId)){
                            finish();
                        }
                    });
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
