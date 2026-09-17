package com.example.modumessenger.Activity;

import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreMember;
import static com.example.modumessenger.Global.GlideUtil.setProfileImage;
import static com.example.modumessenger.entity.ProfileType.*;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.modumessenger.dto.RenameFriendDto;
import com.example.modumessenger.Global.FriendNames;
import com.example.modumessenger.Global.DisplayName;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.text.TextWatcher;
import android.text.Editable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.modumessenger.Global.App;
import com.example.modumessenger.Global.OnSwipeListener;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitChatRoomAPI;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.Retrofit.RetrofitMemberAPI;
import com.example.modumessenger.dto.ChatRoomDto;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.entity.ProfileType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    Long myId;
    Long memberId;
    /** 이 화면이 보여주는 사람. 내 프로필이면 나. */
    Member member;
    /** 로그인한 나 */
    Member myMember;
    boolean isMyInfo = true;

    ImageView profileImageView, wallpaperImageView;
    TextView usernameTextView, statusMessageTextView;
    Button profileEditButton, createChatRoomButton, renameFriendButton;
    ImageButton profileHistoryButton, profileCloseButton;
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

        if (memberId != null) {
            getUserInfo(memberId);
        }
    }

    private void setGestureDetector() {
        gestureDetector = new GestureDetector(this, new OnSwipeListener() {
            @Override
            public boolean onSwipe(Direction direction) {
                if (direction == Direction.up) {
                    //do your stuff
                    Log.d("Swipe Up", "onSwipe: up");
                }

                if (direction == Direction.down) {
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
        renameFriendButton = findViewById(R.id.rename_friend_button);

        profileHistoryButton = findViewById(R.id.profile_image_history_button);

        profileEditButton.setVisibility(View.GONE);
    }

    private void getData() {
        myMember = getDataStoreMember();
        member = myMember;
        memberId = Long.parseLong(getIntent().getStringExtra("memberId"));
        myId = myMember.getId();
    }

    private void setData() {
        retrofitMemberAPI = RetrofitClient.createMemberApiService();
        retrofitChatRoomAPI = RetrofitClient.createChatRoomApiService();

        if (member.getId().equals(memberId)) {
            setUserProfile(member);
            profileEditButton.setVisibility(View.VISIBLE);
            createChatRoomButton.setText("나와 채팅 하기");
        } else {
            getUserInfo(memberId);
            createChatRoomButton.setText("친구와 채팅 하기");
            renameFriendButton.setVisibility(View.VISIBLE);
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
            profileImageIntent(v, PROFILE_WALLPAPER);
        });

        profileImageView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });

        profileImageView.setOnClickListener(v -> {
            profileImageIntent(v, PROFILE_IMAGE);
        });

        profileEditButton.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProfileEditActivity.class);
            startActivity(intent);
        });

        profileCloseButton.setOnClickListener(view -> finish());

        renameFriendButton.setOnClickListener(v -> showRenameDialog());

        createChatRoomButton.setOnClickListener(view -> {
            // 같은 멤버 구성의 방이 있으면 서버가 그 방을 돌려주므로 기존 방으로 이동하게 된다.
            List<Long> ids = new ArrayList<>(Collections.singletonList(myId));

            if (!isMyInfo) {
                ids.add(memberId);
            }

            createChatRoom(ids);
        });

        profileHistoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProfileHistoryActivity.class);
            intent.putExtra("memberId", String.valueOf(member.getId()));
            startActivity(intent);
        });
    }

    private void profileImageIntent(View v, ProfileType type) {
        Intent intent = new Intent(v.getContext(), ProfileImageActivity.class);
        intent.putExtra("memberId", String.valueOf(member.getId()));
        intent.putExtra("profileId", "");
        intent.putExtra("type", type.name());

        startActivity(intent);
    }

    private void setUserProfile(Member member) {
        usernameTextView.setText(DisplayName.of(member.getUserId(), member.getUsername()));
        statusMessageTextView.setText(member.getStatusMessage());

        setProfileImage(profileImageView, member.getProfileImage());
        setProfileImage(wallpaperImageView, member.getWallpaperImage());
    }

    /** 내가 정한 친구 이름 변경. 저장하면 서버와 로컬 별칭 맵을 함께 갱신한다. */
    private void showRenameDialog() {
        if (member == null || isMyInfo) return;
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(DisplayName.of(member.getUserId(), member.getUsername()));
        input.setSelection(input.getText().length());
        FrameLayout wrapper = new FrameLayout(this);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        wrapper.setPadding(pad, 0, pad, 0);
        wrapper.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_Modu_Dialog)
                .setTitle("친구 이름 변경")
                .setView(wrapper)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", (d, w) -> renameFriend(input.getText().toString().trim()))
                .create();
        dialog.setOnShowListener(d -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setEnabled(input.getText().toString().trim().length() > 0);
            input.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { save.setEnabled(s.toString().trim().length() > 0); }
                @Override public void afterTextChanged(Editable s) {}
            });
        });
        dialog.show();
    }

    private void renameFriend(String name) {
        if (name.isEmpty()) return;
        retrofitMemberAPI.RequestRenameFriend(myMember.getUserId(), member.getId(), new RenameFriendDto(name))
                .enqueue(new Callback<MemberDto>() {
                    @Override
                    public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(getApplicationContext(), "이름을 바꾸지 못했습니다. (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        FriendNames.instance().put(member.getUserId(), response.body().getFriendName());
                        usernameTextView.setText(DisplayName.of(member.getUserId(), member.getUsername()));
                        Toast.makeText(getApplicationContext(), "이름을 바꿨습니다.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(@NonNull Call<MemberDto> call, @NonNull Throwable t) {
                        Toast.makeText(getApplicationContext(), "연결에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Retrofit function
    public void getUserInfo(Long id) {
        Call<MemberDto> call = retrofitMemberAPI.RequestMemberById(id);

        call.enqueue(new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        MemberDto memberDto = response.body();
                        member = new Member(memberDto);

                        if (member.getProfiles().size() == 0) {
                            profileHistoryButton.setVisibility(View.GONE);
                        }

                        setUserProfile(member);
                    }
                }

                Log.d("유저 정보 가져오기 요청 : ", Long.toString(id));
            }

            @Override
            public void onFailure(@NonNull Call<MemberDto> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void createChatRoom(List<Long> ids) {
        Call<ChatRoomDto> call = retrofitChatRoomAPI.RequestCreateChatRoom(ids);

        call.enqueue(new Callback<ChatRoomDto>() {
            @Override
            public void onResponse(@NonNull Call<ChatRoomDto> call, @NonNull Response<ChatRoomDto> response) {
                if (!response.isSuccessful()) {
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