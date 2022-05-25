package com.example.modumessenger.Activity;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.modumessenger.Adapter.ChatBubble;
import com.example.modumessenger.Adapter.ChatHistoryAdapter;
import com.example.modumessenger.Adapter.inviteAdapter;
import com.example.modumessenger.Global.ChatWebSocketListener;
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.ChatRoom;
import com.example.modumessenger.Retrofit.Member;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.ChatBubbleViewType;
import com.example.modumessenger.dto.ChatDto;
import com.example.modumessenger.dto.ChatRoomDto;
import com.example.modumessenger.dto.MemberDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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

    ObjectMapper objectMapper;

    List<Member> chatMemberList;
    List<ChatBubble> chatBubbleList;

    RecyclerView recyclerView;
    LinearLayoutManager manager;
    ChatHistoryAdapter chatHistoryAdapter;

    TextView inputMsgTextView;
    Button sendMsg, sendOthers;

    String jwtToken;
    String userId;
    String roomId;

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

        objectMapper = new ObjectMapper();

        chatMemberList = new ArrayList<>();
        chatBubbleList = new ArrayList<>();

        jwtToken = PreferenceManager.getString("token");
        userId = PreferenceManager.getString("userId");
        roomId = getIntent().getStringExtra("roomId");
        if(!roomId.equals("")) {
            getRoomInfo(roomId);
        }

        sendMsg.setOnClickListener(v -> {
            String msg = inputMsgTextView.getText().toString();
            if(msg.length() !=0) {
                ChatDto chatDto = new ChatDto();
                chatDto.setRoomId(roomId);
                chatDto.setMessage(msg);
                chatDto.setSender(userId);
                chatDto.setChatTime(LocalDateTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)));
                chatDto.setChatType(ChatBubbleViewType.RIGHT);

                String message = null;
                try {
                    message = objectMapper.writeValueAsString(chatDto);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                System.out.println(message);

                if(message!=null){
                    webSocket.send(message);

                    ChatBubble chatBubble = new ChatBubble(chatDto);
                    chatHistoryAdapter.addChatMsg(chatBubble);
                    recyclerView.scrollToPosition(chatHistoryAdapter.getItemCount() - 1);

                    inputMsgTextView.setText(null);
                } else {
                    Toast.makeText(getApplicationContext(), "메세지 전송에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ActivityResultLauncher<Intent> resultLauncher;

        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == RESULT_OK){
                        Intent intent = result.getData();
                        Uri uri = intent != null ? intent.getData() : null;

                        if(uri!=null) {
                            // send image to server
                            sendPicture(uri);
                        } else {
                            Toast.makeText(getApplicationContext(), "선택된 이미지가 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        sendOthers.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            resultLauncher.launch(intent);
        });

        this.settingSideNavBar();
    }

    public void settingSideNavBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_dehaze_24);

        DrawerLayout drawLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                ChatActivity.this,
                drawLayout,
                toolbar,
                R.string.open,
                R.string.closed
        );

        drawLayout.addDrawerListener(actionBarDrawerToggle);

        navigationView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();

            if (id == R.id.menu_item1){
                Toast.makeText(getApplicationContext(), "메뉴아이템 1 선택", Toast.LENGTH_SHORT).show();
            }else if(id == R.id.menu_item2){
                Toast.makeText(getApplicationContext(), "메뉴아이템 2 선택", Toast.LENGTH_SHORT).show();
            }else if(id == R.id.menu_item3){
                Toast.makeText(getApplicationContext(), "메뉴아이템 3 선택", Toast.LENGTH_SHORT).show();
            }

            drawLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
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

    private void sendPicture(Uri imgUri) {
        String imagePath = getRealPathFromURI(imgUri);
        File file = new File(imagePath);

        try {
            FileInputStream fileStream = new FileInputStream(imagePath);
            byte[] imageFile = Files.readAllBytes(file.toPath());
            // send image file to server

            fileStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        cursor.moveToFirst();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        return cursor.getString(column_index);
    }

    public void setNavInfo(ChatRoom chatRoom) {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);

        String count = roomInfo.getUserIds().size() + " 명";
        ((TextView) headerView.findViewById(R.id.menu_header_name)).setText(roomInfo.getRoomName());
        ((TextView) headerView.findViewById(R.id.chat_room_member_count)).setText(count);

        Glide.with(this)
                .load(chatRoom.getRoomImage())
                .error(Glide.with(this)
                        .load(R.drawable.basic_profile_image)
                        .into((ImageView)headerView.findViewById(R.id.chat_room_profile_image)))
                .into((ImageView)headerView.findViewById(R.id.chat_room_profile_image));
    }

    // Retrofit function
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
                ChatRoomDto chatRoomDto = response.body();
                roomInfo = new ChatRoom(chatRoomDto);

                setTitle(roomInfo.getRoomName());
                setNavInfo(roomInfo);

                Log.d("채팅방 정보 가져오기 요청 : ", response.body().toString());

                // init web socket
                client = new OkHttpClient();

                Request request = new Request
                        .Builder()
                        .url("ws://192.168.0.3:8080/modu-chat/" + roomId)
                        .addHeader("token", jwtToken)
                        .addHeader("userId", userId)
                        .build();
                listener = new ChatWebSocketListener();
                webSocket = client.newWebSocket(request, listener);

                client.dispatcher().executorService().shutdown();

                if(roomInfo!=null) {
                    getChatRoomMember(roomInfo);
                    getChatList(roomInfo);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChatRoomDto> call, @NonNull Throwable t) {
                Log.e("채팅방 정보 가져오기 요청 실패", t.getMessage());
            }
        });
    }

    public void getChatRoomMember(ChatRoom chatRoom) {
        Call<List<MemberDto>> call = RetrofitClient.getChatApiService().RequestChatRoomMembers(chatRoom.getRoomId());

        call.enqueue(new Callback<List<MemberDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<MemberDto>> call, @NonNull Response<List<MemberDto>> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                assert response.body() != null;
                List<MemberDto> memberList = response.body();
                memberList.forEach(m -> {
                    chatMemberList.add(new Member(m));
                });

                Log.d("채팅방 멤버 리스트 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<List<MemberDto>> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
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
                chatHistory.forEach(c->{
                    chatBubbleList.add(new ChatBubble(c));
                });

                chatHistoryAdapter = new ChatHistoryAdapter(chatBubbleList, chatMemberList);
                recyclerView.setAdapter(chatHistoryAdapter);
                recyclerView.scrollToPosition(chatHistoryAdapter.getItemCount() - 1);

                Log.d("채팅 내역 가져오기 요청 : ", chatRoom.getRoomId());
            }

            @Override
            public void onFailure(@NonNull Call<List<ChatDto>> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }
}
