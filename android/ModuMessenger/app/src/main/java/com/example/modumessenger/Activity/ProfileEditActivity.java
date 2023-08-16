package com.example.modumessenger.Activity;

import static com.example.modumessenger.Activity.ProfileEditBottomSheetFragment.*;
import static com.example.modumessenger.Global.DataStoreHelper.*;
import static com.example.modumessenger.Global.GlideUtil.setProfileImage;

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

import com.example.modumessenger.Global.ScopedStorageUtil;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitImageAPI;
import com.example.modumessenger.Retrofit.RetrofitMemberAPI;
import com.example.modumessenger.Retrofit.RetrofitProfileAPI;
import com.example.modumessenger.dto.CreateProfileDto;
import com.example.modumessenger.dto.ProfileDto;
import com.example.modumessenger.dto.UpdateProfileDto;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.entity.ProfileType;
import com.google.gson.Gson;

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
    RetrofitProfileAPI retrofitProfileAPI;
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
        getMyProfileInfo(member.getEmail());
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

            updateProfile(new UpdateProfileDto(member));

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
        member = getDataStoreMember();
    }

    private void setData() {
        retrofitMemberAPI = RetrofitClient.createMemberApiService();
        retrofitProfileAPI = RetrofitClient.createProfileApiService();
        retrofitImageAPI = RetrofitClient.createImageApiService();

        scopedStorageUtil = new ScopedStorageUtil();

        setUserProfile(member);
    }

    private void setButtonClickEvent() {
        myProfileSaveButton.setOnClickListener(v -> {
            boolean isUpdate = false;

            if(!member.getUsername().equals(myProfileName.getText().toString())){
                isUpdate = true;
            }

            if(!member.getStatusMessage().equals(myStatusMessage.getText().toString())) {
                isUpdate = true;
                addProfile(new CreateProfileDto(member.getId(), ProfileType.PROFILE_STATUS_MESSAGE, myStatusMessage.getText().toString()));
            }

            if(isUpdate) {
                member.setUsername(myProfileName.getText().toString());
                member.setStatusMessage(myStatusMessage.getText().toString());

                updateProfile(new UpdateProfileDto(member));
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

        uploadProfileImage(filePart, event);
    }

    private String getFileName(ContentResolver resolver, Uri uri) {
        Cursor cursor = resolver.query(uri, null, null, null, null);
        int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        String fileName = cursor.getString(columnIndex);
        cursor.close();

        return fileName;
    }

    private void setTextOnView(TextView view, String value) {
        if(value != null && !value.equals("")) {
            view.setText(value);
        } else {
            view.setText("No Value");
        }
    }

    private void setUserProfile(Member member) {
        setTextOnView(myProfileName, member.getUsername());
        setTextOnView(myStatusMessage, member.getStatusMessage());

        setProfileImage(profileImageView, member.getProfileImage());
        setProfileImage(wallpaperImageView, member.getWallpaperImage());
    }

    // Retrofit function
    public void addProfile(CreateProfileDto createProfileDto) {
        Call<ProfileDto> profileDtoCall = retrofitProfileAPI.RequestCreateProfile(createProfileDto);

        profileDtoCall.enqueue(new Callback<ProfileDto>() {
            @Override
            public void onResponse(@NonNull Call<ProfileDto> call, @NonNull Response<ProfileDto> response) {
                if (response.isSuccessful()) {
                    if(response.body() != null) {
                        updateProfile(new UpdateProfileDto(member));

                        Log.d("프로필 생성 요청 : ", response.body().toString());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProfileDto> call, @NonNull Throwable t) {
                Log.e("연결 실패", t.getMessage());
            }
        });
    }

    public void updateProfile(UpdateProfileDto updateProfileDto) {
        Call<MemberDto> call = retrofitMemberAPI.RequestUpdateProfile(member.getUserId(), updateProfileDto);

        call.enqueue(new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {

                if(response.isSuccessful()) {
                    if(response.body() != null) {
                        MemberDto myInfo = response.body();

                        setUserProfile(member);

                        String json = new Gson().toJson(myInfo);
                        setDataStoreObject("member", json);

                        Toast.makeText(getApplicationContext(), "프로필 정보가 업데이트 되었습니다.", Toast.LENGTH_SHORT).show();
                        Log.d("내정보 업데이트 요청 : ", response.body().toString());

                        finish();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<MemberDto> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void getMyProfileInfo(String email) {
        Call<MemberDto> call = retrofitMemberAPI.RequestUserInfo(email);

        call.enqueue(new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                if(response.isSuccessful()) {
                    if(response.body() != null) {
                        MemberDto memberDto = response.body();
                        member.updateProfile(memberDto);

                        setUserProfile(member);

                        String json = new Gson().toJson(member);
                        setDataStoreObject("member", json);

                        Log.d("내 정보 가져오기 요청 : ", response.body().toString());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<MemberDto> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void uploadProfileImage(MultipartBody.Part file, int event) {
        Call<String> call = retrofitImageAPI.RequestUploadImage(file);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                scopedStorageUtil.deleteTempFiles();

                if(response.isSuccessful()) {
                    if(response.body() != null) {
                        String fileName = response.body();

                        String username = String.valueOf(myProfileName.getText());
                        String statusMessage = String.valueOf(myStatusMessage.getText());

                        ProfileType type = null;

                        if (event == PROFILE_EVENT.PROFILE_CHANGE.getEvent()) {
                            type = ProfileType.PROFILE_IMAGE;
                        } else if(event == PROFILE_EVENT.WALLPAPER_CHANGE.getEvent()) {
                            type = ProfileType.PROFILE_WALLPAPER;
                        }

                        member.updateProfile(
                                username.equals("null") ? null : username,
                                statusMessage.equals("null") ? null : statusMessage,
                                event == PROFILE_EVENT.PROFILE_CHANGE.getEvent() ? fileName : member.getProfileImage(),
                                event == PROFILE_EVENT.WALLPAPER_CHANGE.getEvent() ? fileName : member.getWallpaperImage()
                        );

                        addProfile(new CreateProfileDto(member.getId(), type, fileName));

                        Log.d("프로필 이미지 업로드 요청 : ", response.body());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }
}