package com.example.modumessenger.Activity;

import static android.os.SystemClock.sleep;

import static com.example.modumessenger.Activity.ProfileEditBottomSheetFragment.*;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.Global.ScopedStorageUtil;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitChatRoomAPI;
import com.example.modumessenger.Retrofit.RetrofitImageAPI;
import com.example.modumessenger.Retrofit.RetrofitMemberAPI;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.MemberDto;

import java.io.File;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileEditActivity extends AppCompatActivity implements ProfileEditBottomSheetListener {

    Member member;
    ImageView profileImageView, wallpaperImageView;
    EditText myProfileName, myStatusMessage;
    Button profileCloseButton, myProfileSaveButton;
    ImageButton profileEditButton, wallpaperEditButton;

    ActivityResultLauncher<Intent> profileLauncher, wallpaperLauncher;

    ScopedStorageUtil scopedStorageUtil;

    RetrofitMemberAPI retrofitMemberAPI;
    RetrofitImageAPI retrofitImageAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        bindingView();
        setLauncher();
        getData();
        setData();
        setButtonClickEvent();
    }

    @Override
    public void onResume() {
        super.onResume();
        getMyProfileInfo(new MemberDto(PreferenceManager.getString("userId"), PreferenceManager.getString("email")));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        scopedStorageUtil.deleteTempFiles();
    }

    @Override
    public void onButtonClicked(int event) {
        if(event == PROFILE_EVENT.PROFILE_CHANGE.getEvent() || event == PROFILE_EVENT.WALLPAPER_CHANGE.getEvent()) {
            Toast.makeText(this, "갤러리로 이동합니다", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent();

            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpg", "image/jpeg", "image/png"});
            intent.setAction(Intent.ACTION_GET_CONTENT);

            if(event == PROFILE_EVENT.PROFILE_CHANGE.getEvent()) {
                profileLauncher.launch(intent);
            } else {
                wallpaperLauncher.launch(intent);
            }
        } else if(event == PROFILE_EVENT.PROFILE_DEFAULT.getEvent() || event == PROFILE_EVENT.WALLPAPER_DEFAULT.getEvent()) {
            Toast.makeText(this, "기본 이미지로 변경합니다", Toast.LENGTH_SHORT).show();

            if(event == PROFILE_EVENT.PROFILE_DEFAULT.getEvent()) {
                member.setProfileImage("");
            } else {
                member.setWallpaperImage("");
            }

            updateMyInfo(new MemberDto(member));

            if(event == PROFILE_EVENT.PROFILE_DEFAULT.getEvent()) {
                setProfileImage(profileImageView, member.getProfileImage());
            } else {
                setProfileImage(wallpaperImageView, member.getWallpaperImage());
            }
        }
    }

    private void bindingView() {
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).hide();

        wallpaperImageView = findViewById(R.id.profile_wallpaper_image);
        profileImageView = findViewById(R.id.my_profile_activity_image);
        myProfileName = findViewById(R.id.my_profile_name);
        myStatusMessage = findViewById(R.id.my_status_message);

        profileCloseButton = findViewById(R.id.profile_close_button);
        myProfileSaveButton = findViewById(R.id.my_profile_save_button);

        profileEditButton = findViewById(R.id.my_profile_image_edit_button);
        wallpaperEditButton = findViewById(R.id.my_wallpaper_edit_button);

        profileImageView.bringToFront();
        profileEditButton.bringToFront();
    }

    private void setLauncher() {
        profileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        updateProfileInfo(result, PROFILE_EVENT.PROFILE_CHANGE.getEvent());
                    }
                });

        wallpaperLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        updateProfileInfo(result, PROFILE_EVENT.WALLPAPER_CHANGE.getEvent());
                    }
                });
    }

    public void updateProfileInfo(ActivityResult result, int event) {
        Intent intent = result.getData();
        Uri uri = intent != null ? intent.getData() : null;
        String fileName = getFileName(getContentResolver(), uri);
        if(fileName!=null) {
            String filePath = scopedStorageUtil.copyFromScopedStorage(this, uri, fileName);
            changeMyProfileImage(filePath, event);
        } else {
            Log.d("파일명 가져오기 실패 : ", "갤러리 에서 가져오기 실패");
        }
    }

    private void getData() {
        if(getIntentExtra("profileImage") && getIntentExtra("username") && getIntentExtra("statusMessage")) {
            member = new Member(PreferenceManager.getString("userId"), PreferenceManager.getString("email"));
            member.setUsername(getIntent().getStringExtra("username"));
            member.setStatusMessage(getIntent().getStringExtra("statusMessage"));
            member.setProfileImage(getIntent().getStringExtra("profileImage"));
            member.setWallpaperImage(getIntent().getStringExtra("wallpaperImage"));
        } else {
            Toast.makeText(this, "No Data", Toast.LENGTH_SHORT).show();
        }
    }

    private void setData() {
        String accessToken = PreferenceManager.getString("access-token");
        retrofitMemberAPI = RetrofitClient.createMemberApiService(accessToken);
        retrofitImageAPI = RetrofitClient.createImageApiService(accessToken);

        scopedStorageUtil = new ScopedStorageUtil();
        setTextOnView(myProfileName, member.getUsername());
        setTextOnView(myStatusMessage, member.getStatusMessage());

        setProfileImage(profileImageView, member.getProfileImage());
        setProfileImage(wallpaperImageView, member.getWallpaperImage());
    }

    private void setButtonClickEvent() {
        myProfileSaveButton.setOnClickListener(v -> {
            if(!member.getUsername().equals(myProfileName.getText().toString()) ||
                    !member.getStatusMessage().equals(myStatusMessage.getText().toString())) {

                member.setUsername(myProfileName.getText().toString());
                member.setStatusMessage(myStatusMessage.getText().toString());

                updateMyInfo(new MemberDto(member));
            }
        });

        profileEditButton.setOnClickListener(view -> {
            ProfileEditBottomSheetFragment bottomSheetDialog = new ProfileEditBottomSheetFragment(PROFILE_IMAGE_TYPE.PROFILE_IMAGE.getType());
            bottomSheetDialog.show(getSupportFragmentManager(), bottomSheetDialog.getTag());
        });

        wallpaperEditButton.setOnClickListener(view -> {
            ProfileEditBottomSheetFragment bottomSheetDialog = new ProfileEditBottomSheetFragment(PROFILE_IMAGE_TYPE.PROFILE_WALLPAPER.getType());
            bottomSheetDialog.show(getSupportFragmentManager(), bottomSheetDialog.getTag());
        });

        profileCloseButton.setOnClickListener(view -> finish());
    }

    private void changeMyProfileImage(String filePath, int event) {
        File file = new File(filePath);
        RequestBody fileBody = RequestBody.Companion.create(file, MediaType.parse("multipart/form-data"));
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), fileBody);

        String originProfileImage;

        if(event == PROFILE_EVENT.PROFILE_CHANGE.getEvent()) {
            originProfileImage = member.getProfileImage();
        } else {
            originProfileImage = member.getWallpaperImage();
        }

        uploadProfileImage(filePart, originProfileImage, event);
    }

    private String getFileName(ContentResolver resolver, Uri uri) {
        Cursor cursor = resolver.query(uri, null, null, null, null);
        int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        String fileName = cursor.getString(columnIndex);
        cursor.close();

        return fileName;
    }

    private boolean getIntentExtra(String key) {
        return getIntent().hasExtra(key);
    }

    private void setProfileImage(ImageView imageView, String imageUrl) {
        Glide.with(this)
                .load(imageUrl==null || imageUrl.equals("") ? R.drawable.basic_profile_image : imageUrl)
                .error(Glide.with(this)
                        .load(R.drawable.basic_profile_image)
                        .into(imageView))
                .into(imageView);
    }

    private void setDefaultProfileImage() {
        Glide.with(this)
                .load(R.drawable.basic_profile_image)
                .into(profileImageView);
    }

    private void setTextOnView(TextView view, String value) {
        if(value != null && !value.equals("")) {
            view.setText(value);
        } else {
            view.setText("No Value");
        }
    }

    // Retrofit function
    public void updateMyInfo(MemberDto memberDto) {
        Call<MemberDto> call = retrofitMemberAPI.RequestUpdate(member.getUserId(), memberDto);

        call.enqueue(new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                try {
                    assert response.body() != null;
                    MemberDto myInfo = response.body();
                    member = new Member(myInfo);

                    setProfileImage(profileImageView, member.getProfileImage());
                    setProfileImage(wallpaperImageView, member.getWallpaperImage());

                    Log.d("내정보 업데이트 요청 : ", response.body().toString());

                    Toast.makeText(getApplicationContext(), "프로필 정보가 업데이트 되었습니다.", Toast.LENGTH_SHORT).show();

                    PreferenceManager.setString("profileImage", member.getProfileImage());
                    PreferenceManager.setString("wallpaperImage", member.getWallpaperImage());

                    finish();
                } catch (Exception e) {
                    Log.e("오류 발생 : ", e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<MemberDto> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void getMyProfileInfo(MemberDto memberDto) {
        Call<MemberDto> call = retrofitMemberAPI.RequestUserInfo(memberDto);

        call.enqueue(new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                MemberDto result = response.body();

                assert response.body() != null;
                assert result != null;

                // get my Profile Info
                myProfileName.setText(result.getUsername());
                myStatusMessage.setText(result.getStatusMessage());

                setProfileImage(profileImageView, result.getProfileImage());
                setProfileImage(wallpaperImageView, result.getWallpaperImage());

                member.setProfileImage(result.getProfileImage());
                member.setWallpaperImage(result.getWallpaperImage());

                if(member.getEmail().equals(result.getEmail())){
                    Log.d("중복검사: ", "중복된 번호가 아닙니다");
                }

                Log.d("내 정보 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<MemberDto> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void uploadProfileImage(MultipartBody.Part file, String originProfileImage, int event) {
        Call<String> call = retrofitImageAPI.RequestUploadImage(file);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                }

                assert response.body() != null;
                String filePath = response.body();

                if(event == PROFILE_EVENT.PROFILE_CHANGE.getEvent()) {
                    member.setProfileImage(RetrofitClient.getBaseUrl() + "modu_chat/images/" + filePath);
                } else {
                    member.setWallpaperImage(RetrofitClient.getBaseUrl() + "modu_chat/images/" + filePath);
                }

                scopedStorageUtil.deleteTempFiles();

                if(!member.getProfileImage().equals(originProfileImage) || !member.getWallpaperImage().equals(originProfileImage)) {
                    member.setUsername(String.valueOf(myProfileName.getText()));
                    member.setStatusMessage(String.valueOf(myStatusMessage.getText()));

                    sleep(1000);

                    updateMyInfo(new MemberDto(member));
                }

                Log.d("프로필 이미지 업로드 요청 : ", response.body());
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
                member.setProfileImage(originProfileImage);
            }
        });
    }
}