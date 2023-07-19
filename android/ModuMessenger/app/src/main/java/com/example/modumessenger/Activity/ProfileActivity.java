package com.example.modumessenger.Activity;

import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreMember;
import static com.example.modumessenger.Global.GlideUtil.setProfileImage;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.modumessenger.Global.OnSwipeListener;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitChatRoomAPI;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.Retrofit.RetrofitMemberAPI;
import com.example.modumessenger.dto.ChatRoomDto;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.entity.Profile;
import com.example.modumessenger.entity.ProfileType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    String userId;
    String email;
    Member myInfo;
    Member member;
    boolean isMyInfo = true;

    ImageView profileImageView, wallpaperImageView;
    TextView usernameTextView, statusMessageTextView;
    Button profileEditButton, profileCloseButton, createChatRoomButton;
    GestureDetector gestureDetector;

    RetrofitMemberAPI retrofitMemberAPI;
    RetrofitChatRoomAPI retrofitChatRoomAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        setGestureDetector();
        bindingView();
        getData();
        setData();
        setButtonClickEvent();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(email != null && !email.equals("")) {
            getUserInfo(email);
        }
    }

    private void setGestureDetector() {
        gestureDetector = new GestureDetector(this, new OnSwipeListener(){
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

                return false;
            }
        });
    }

    private void bindingView() {
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).hide();

        wallpaperImageView = findViewById(R.id.profile_wallpaper_image);
        profileImageView = findViewById(R.id.profile_activity_image);
        usernameTextView = findViewById(R.id.profile_activity_username);
        statusMessageTextView = findViewById(R.id.profile_activity_status_message);

        profileEditButton = findViewById(R.id.profile_edit_button);
        profileCloseButton = findViewById(R.id.profile_close_button);
        createChatRoomButton = findViewById(R.id.start_chat_button);

        profileEditButton.setVisibility(View.INVISIBLE);
    }

    private void getData() {
        myInfo = getDataStoreMember();
        userId = getIntent().getStringExtra("userId");
        email = getIntent().getStringExtra("email");
    }

    private void setData() {
        retrofitMemberAPI = RetrofitClient.createMemberApiService();
        retrofitChatRoomAPI = RetrofitClient.createChatRoomApiService();

        if (myInfo.getEmail().equals(email)) {
            setUserProfile(myInfo);
            profileEditButton.setVisibility(View.VISIBLE);
            createChatRoomButton.setText("나와 채팅 하기");
        } else {
            getUserInfo(email);
            createChatRoomButton.setText("친구와 채팅 하기");
            isMyInfo = false;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setButtonClickEvent() {
        wallpaperImageView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });

        wallpaperImageView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProfileImageActivity.class);
            intent.putExtra("email", email);

            List<String> imageFileList = member.getProfileList()
                    .stream()
                    .filter(profile -> profile.getProfileType().equals(ProfileType.PROFILE_WALLPAPER))
                    .sorted(Comparator.comparing(Profile::getLastModifiedDateTime).reversed())
                    .map(Profile::getValue)
                    .collect(Collectors.toList());

            intent.putStringArrayListExtra("imageFileList", new ArrayList<>(imageFileList));
            startActivity(intent);
        });

        profileImageView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });

        profileImageView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProfileImageActivity.class);
            intent.putExtra("email", member.getEmail());

            List<String> imageFileList = member.getProfileList()
                    .stream()
                    .filter(profile -> profile.getProfileType().equals(ProfileType.PROFILE_IMAGE))
                    .sorted(Comparator.comparing(Profile::getLastModifiedDateTime).reversed())
                    .map(Profile::getValue)
                    .collect(Collectors.toList());

            intent.putStringArrayListExtra("imageFileList", new ArrayList<>(imageFileList));
            startActivity(intent);
        });

        profileEditButton.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProfileEditActivity.class);
            startActivity(intent);
        });

        profileCloseButton.setOnClickListener(view -> finish());

        createChatRoomButton.setOnClickListener(view -> {
            // exist chat room
            // if not, create chat room
            List<String> userIds = new ArrayList<>(Collections.singletonList(userId));

            if(!isMyInfo) {
                userIds.add(myInfo.getUserId());
            }

            createChatRoom(userIds);
        });
    }

    private boolean getIntentExtra(String key) {
        return getIntent().hasExtra(key);
    }

    private void setTextOnView(TextView view, String value) {
        if(value != null && !value.equals("")) {
            view.setText(value);
        } else {
            view.setText("");
        }
    }

    private void setImageOnView(ImageView view, int value) {
        view.setImageResource(value);
    }

    private void setUserProfile(Member member) {
        usernameTextView.setText(member.getUsername());
        statusMessageTextView.setText(member.getStatusMessage());

        setProfileImage(profileImageView, member.getProfileImage());
        setProfileImage(wallpaperImageView, member.getWallpaperImage());

        profileImageView.bringToFront();
    }

    // Retrofit function
    public void getUserInfo(String email) {
        Call<MemberDto> call = retrofitMemberAPI.RequestUserInfo(email);

        call.enqueue(new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                if(response.isSuccessful()) {
                    if(response.body() != null) {
                        MemberDto memberDto = response.body();
                        member = new Member(memberDto);

                        setUserProfile(member);
                    }
                }

                Log.d("유저 정보 가져오기 요청 : ", email);
            }

            @Override
            public void onFailure(@NonNull Call<MemberDto> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void createChatRoom(List<String> userIds) {
        Call<ChatRoomDto> call = retrofitChatRoomAPI.RequestCreateChatRoom(userIds);

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