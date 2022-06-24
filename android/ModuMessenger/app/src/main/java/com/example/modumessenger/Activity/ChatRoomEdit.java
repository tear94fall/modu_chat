package com.example.modumessenger.Activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.modumessenger.Global.OnSwipeListener;
import com.example.modumessenger.R;
import com.example.modumessenger.entity.ChatRoom;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.ChatRoomDto;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRoomEdit extends AppCompatActivity implements View.OnTouchListener {

    ChatRoom roomInfo;
    String roomId;

    ImageView chatRoomImageView;
    EditText chatRoomName;
    Button chatRoomEditCloseButton, chatRoomImageChangeButton, chatRoomSaveButton;

    GestureDetector gestureDetector;

    ActivityResultLauncher<Intent> launcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room_edit);

        getData();
        bindingView();
        setLauncher();
        setButtonClickEvent();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(event.toString(), "onTouch: ");
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private void getData() {
        roomId = getIntent().getStringExtra("roomId");
        if(roomId != null && !roomId.equals("")) {
            getRoomInfo(roomId);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindingView() {
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).hide();

        chatRoomImageView = findViewById(R.id.chat_room_image);
        chatRoomImageView.setOnTouchListener(this);

        chatRoomName = findViewById(R.id.chat_room_name);
        chatRoomEditCloseButton = findViewById(R.id.chat_room_edit_close_button);
        chatRoomImageChangeButton = findViewById(R.id.chat_room_image_change_button);
        chatRoomSaveButton = findViewById(R.id.chat_room_save_button);
    }

    private void setLauncher() {
        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if(result.getData()!=null && result.getData().getData() != null) {
                            Glide.with(this)
                                    .load(result.getData().getData())
                                    .error(Glide.with(this)
                                            .load(R.drawable.basic_profile_image)
                                            .into(chatRoomImageView))
                                    .into(chatRoomImageView);
                        }
                    }
                });
    }

    private void setButtonClickEvent() {
        gestureDetector = new GestureDetector(this,new OnSwipeListener(){
            @Override
            public boolean onSwipe(Direction direction) {
                if (direction==Direction.up){
                    //do your stuff
                    Log.d("Swipe Up", "onSwipe: up");
                }

                if (direction==Direction.down){
                    //do your stuff
                    Log.d("Swipe Down", "onSwipe: down");
                    finish();
                }
                return true;
            }
        });

        chatRoomEditCloseButton.setOnClickListener(v -> {
            finish();
        });

        chatRoomSaveButton.setOnClickListener(v -> {
            if(!roomInfo.getRoomName().equals(chatRoomName.getText().toString())) {
                roomInfo.setRoomName(chatRoomName.getText().toString());
                updateChatRoomInfo(new ChatRoomDto(roomInfo));
            }
        });

        chatRoomImageChangeButton.setOnClickListener(view -> {
            final PopupMenu popupMenu = new PopupMenu(getApplicationContext(),view);
            getMenuInflater().inflate(R.menu.profile_image_popup,popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == R.id.action_menu1){
                    Toast.makeText(this, "갤러리로 이동합니다", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    launcher.launch(intent);

                }else if (menuItem.getItemId() == R.id.action_menu2){
                    Toast.makeText(this, "기본 이미지로 변경합니다", Toast.LENGTH_SHORT).show();
                    roomInfo.setRoomImage("");
                    setDefaultProfileImage();
                }else {
                    Toast.makeText(this, "프로필 이미지 변경", Toast.LENGTH_SHORT).show();
                }

                return false;
            });
            popupMenu.show();
        });
    }

    private void setDefaultProfileImage() {
        Glide.with(this)
                .load(R.drawable.basic_profile_image)
                .into(chatRoomImageView);
    }

    // Retrofit function
    public void getRoomInfo(String roomId) {
        Call<ChatRoomDto> call = RetrofitClient.getChatRoomApiService().RequestChatRoom(roomId);

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

                chatRoomName.setText(roomInfo.getRoomName());

                Glide.with(getApplicationContext())
                        .load(roomInfo.getRoomImage().equals("") ? R.drawable.basic_profile_image : roomInfo.getRoomImage())
                        .error(Glide.with(getApplicationContext())
                                .load(R.drawable.basic_profile_image)
                                .into(chatRoomImageView))
                        .into(chatRoomImageView);

                Log.d("채팅방 정보 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<ChatRoomDto> call, @NonNull Throwable t) {
                Log.e("채팅방 정보 가져오기 요청 실패", t.getMessage());
            }
        });
    }

    public void updateChatRoomInfo(ChatRoomDto chatRoomDto) {
        Call<ChatRoomDto> call = RetrofitClient.getChatRoomApiService().RequestUpdateChatRoom(roomId, chatRoomDto);

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

                chatRoomName.setText(roomInfo.getRoomName());
                Glide.with(getApplicationContext())
                        .load(roomInfo.getRoomImage())
                        .error(Glide.with(getApplicationContext())
                                .load(R.drawable.basic_profile_image)
                                .into(chatRoomImageView))
                        .into(chatRoomImageView);

                Log.d("채팅방 정보 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<ChatRoomDto> call, @NonNull Throwable t) {
                Log.e("채팅방 정보 가져오기 요청 실패", t.getMessage());
            }
        });
    }
}