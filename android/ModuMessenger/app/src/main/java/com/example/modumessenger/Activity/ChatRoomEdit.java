package com.example.modumessenger.Activity;

import static com.example.modumessenger.Global.GlideUtil.setBasicProfileImage;
import static com.example.modumessenger.Global.GlideUtil.setProfileImage;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.modumessenger.Global.OnSwipeListener;
import com.example.modumessenger.Global.ScopedStorageUtil;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitChatRoomAPI;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.Retrofit.RetrofitImageAPI;
import com.example.modumessenger.dto.ChatRoomDto;
import com.example.modumessenger.entity.ChatRoom;

import java.io.File;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRoomEdit extends AppCompatActivity implements View.OnTouchListener {

    ChatRoom roomInfo;
    String roomId;

    /**
     * 사진을 새로 고르거나 기본 이미지로 되돌렸을 때만 값이 들어간다.
     * null 이면 사진은 건드리지 않은 것이다. 예전에는 고른 사진을 화면에만 그리고
     * 올리지도, 저장하지도 않아 사진 변경이 아무 효과가 없었다.
     */
    String pendingRoomImage;

    ImageView chatRoomImageView;
    EditText chatRoomName;
    Button chatRoomSaveButton;
    ImageButton chatRoomEditCloseButton, chatRoomImageChangeButton;

    GestureDetector gestureDetector;

    ActivityResultLauncher<Intent> launcher;

    ScopedStorageUtil scopedStorageUtil;
    RetrofitChatRoomAPI retrofitChatRoomAPI;
    RetrofitImageAPI retrofitImageAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room_edit);

        // 방 정보 조회 응답이 뷰를 건드리므로 바인딩을 먼저 한다.
        bindingView();
        getData();
        setData();
        setLauncher();
        setButtonClickEvent();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private void getData() {
        retrofitChatRoomAPI = RetrofitClient.createChatRoomApiService();
        retrofitImageAPI = RetrofitClient.createImageApiService();
        scopedStorageUtil = new ScopedStorageUtil();
    }

    private void setData() {
        roomId = getIntent().getStringExtra("roomId");
        if (roomId != null && !roomId.equals("")) {
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
                    if (result.getResultCode() != RESULT_OK) {
                        return;
                    }

                    Intent data = result.getData();
                    Uri uri = data != null ? data.getData() : null;
                    if (uri == null) {
                        return;
                    }

                    uploadPickedImage(uri);
                });
    }

    private void setButtonClickEvent() {
        gestureDetector = new GestureDetector(this, new OnSwipeListener() {
            @Override
            public boolean onSwipe(Direction direction) {
                if (direction == Direction.down) {
                    finish();
                }
                return true;
            }
        });

        chatRoomEditCloseButton.setOnClickListener(v -> finish());

        chatRoomSaveButton.setOnClickListener(v -> saveRoomInfo());

        chatRoomImageChangeButton.setOnClickListener(view -> {
            final PopupMenu popupMenu = new PopupMenu(this, view);
            getMenuInflater().inflate(R.menu.profile_image_popup, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == R.id.action_menu1) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    launcher.launch(intent);
                } else if (menuItem.getItemId() == R.id.action_menu2) {
                    pendingRoomImage = "";
                    setBasicProfileImage(chatRoomImageView);
                    Toast.makeText(this, "기본 이미지로 바꿨습니다. 저장을 눌러 주세요.", Toast.LENGTH_SHORT).show();
                }

                return false;
            });
            popupMenu.show();
        });
    }

    /**
     * 이름과 사진 중 하나라도 바뀌었으면 저장한다. 예전에는 이름이 바뀐 경우에만
     * 저장이 호출되어 사진만 바꾸면 아무 일도 일어나지 않았다.
     */
    private void saveRoomInfo() {
        if (roomInfo == null) {
            Toast.makeText(this, "채팅방 정보를 아직 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String newName = chatRoomName.getText().toString().trim();
        String currentName = roomInfo.getRoomName() == null ? "" : roomInfo.getRoomName();
        String currentImage = roomInfo.getRoomImage() == null ? "" : roomInfo.getRoomImage();

        boolean nameChanged = !newName.equals(currentName);
        boolean imageChanged = pendingRoomImage != null && !pendingRoomImage.equals(currentImage);

        if (!nameChanged && !imageChanged) {
            Toast.makeText(this, "변경된 내용이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nameChanged) {
            roomInfo.setRoomName(newName);
        }

        if (imageChanged) {
            roomInfo.setRoomImage(pendingRoomImage);
        }

        updateChatRoomInfo(new ChatRoomDto(roomInfo));
    }

    private void uploadPickedImage(Uri uri) {
        String fileName = getFileName(getContentResolver(), uri);
        if (fileName == null) {
            Toast.makeText(this, "사진을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String filePath = scopedStorageUtil.copyFromScopedStorage(this, uri, fileName);
        File file = new File(filePath);

        RequestBody fileBody = RequestBody.Companion.create(file, MediaType.parse("multipart/form-data"));
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), fileBody);

        uploadRoomImage(filePart);
    }

    private String getFileName(ContentResolver resolver, Uri uri) {
        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }

            int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            return columnIndex < 0 ? null : cursor.getString(columnIndex);
        }
    }

    private void setRoomInfoOnView(ChatRoom room) {
        setTitle(room.getRoomName());
        chatRoomName.setText(room.getRoomName());

        // 방 사진도 프로필 사진과 같은 경로로 불러온다. 예전에는 파일명을 그대로
        // 주소처럼 넘겨 저장된 사진이 화면에 나오지 않았다.
        setProfileImage(chatRoomImageView, room.getRoomImage());
    }

    // Retrofit function
    public void getRoomInfo(String roomId) {
        Call<ChatRoomDto> call = retrofitChatRoomAPI.RequestChatRoom(roomId);

        call.enqueue(new Callback<ChatRoomDto>() {
            @Override
            public void onResponse(@NonNull Call<ChatRoomDto> call, @NonNull Response<ChatRoomDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    Toast.makeText(ChatRoomEdit.this, "채팅방 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                roomInfo = new ChatRoom(response.body());
                setRoomInfoOnView(roomInfo);

                Log.d("채팅방 정보 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<ChatRoomDto> call, @NonNull Throwable t) {
                Log.e("채팅방 정보 가져오기 요청 실패", t.getMessage());
                Toast.makeText(ChatRoomEdit.this, "채팅방 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void uploadRoomImage(MultipartBody.Part file) {
        Call<String> call = retrofitImageAPI.RequestUploadImage(file);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                scopedStorageUtil.deleteTempFiles();

                if (!response.isSuccessful() || response.body() == null) {
                    Log.e("사진 올리기 실패 : ", "error code : " + response.code());
                    Toast.makeText(ChatRoomEdit.this, "사진을 올리지 못했습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                pendingRoomImage = response.body();
                setProfileImage(chatRoomImageView, pendingRoomImage);

                Toast.makeText(ChatRoomEdit.this, "사진을 바꿨습니다. 저장을 눌러 주세요.", Toast.LENGTH_SHORT).show();
                Log.d("채팅방 사진 올리기 요청 : ", pendingRoomImage);
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                scopedStorageUtil.deleteTempFiles();
                Log.e("사진 올리기 실패", t.getMessage());
                Toast.makeText(ChatRoomEdit.this, "사진을 올리지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void updateChatRoomInfo(ChatRoomDto chatRoomDto) {
        Call<ChatRoomDto> call = retrofitChatRoomAPI.RequestUpdateChatRoom(roomId, chatRoomDto);

        call.enqueue(new Callback<ChatRoomDto>() {
            @Override
            public void onResponse(@NonNull Call<ChatRoomDto> call, @NonNull Response<ChatRoomDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    Toast.makeText(ChatRoomEdit.this, "저장하지 못했습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                roomInfo = new ChatRoom(response.body());
                pendingRoomImage = null;
                setRoomInfoOnView(roomInfo);

                Toast.makeText(ChatRoomEdit.this, "채팅방 정보를 저장했습니다.", Toast.LENGTH_SHORT).show();
                Log.d("채팅방 정보 저장 요청 : ", response.body().toString());

                // 저장하고도 이 화면에 머물러 바뀐 것이 없어 보였다. 닫아서 방 화면과
                // 목록이 다시 불러오게 한다.
                finish();
            }

            @Override
            public void onFailure(@NonNull Call<ChatRoomDto> call, @NonNull Throwable t) {
                Log.e("채팅방 정보 저장 요청 실패", t.getMessage());
                Toast.makeText(ChatRoomEdit.this, "저장하지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
