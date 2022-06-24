package com.example.modumessenger.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.MemberDto;

import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileEditActivity extends AppCompatActivity {

    Member member;
    ImageView profileImageView;
    EditText myProfileName, myStatusMessage;
    Button profileCloseButton, changeProfileButton, myProfileSaveButton;

    ActivityResultLauncher<Intent> launcher;

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

    private void bindingView() {
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).hide();

        profileImageView = findViewById(R.id.my_profile_activity_image);
        myProfileName = findViewById(R.id.my_profile_name);
        myStatusMessage = findViewById(R.id.my_status_message);

        profileCloseButton = findViewById(R.id.profile_close_button);
        changeProfileButton = findViewById(R.id.my_profile_image_change_button);
        myProfileSaveButton = findViewById(R.id.my_profile_save_button);
    }

    private void setLauncher() {
        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent intent = result.getData();
                        if(intent!=null) {
                            Uri uri = intent.getData();
                            changeMyProfileImage(uri);
                        }
                    }
                });
    }

    private void getData() {
        if(getIntentExtra("profileImage") && getIntentExtra("username") && getIntentExtra("statusMessage")) {
            member = new Member(PreferenceManager.getString("userId"), PreferenceManager.getString("email"));
            member.setUsername(getIntent().getStringExtra("username"));
            member.setStatusMessage(getIntent().getStringExtra("statusMessage"));
            member.setProfileImage(getIntent().getStringExtra("profileImage"));
        } else {
            Toast.makeText(this, "No Data", Toast.LENGTH_SHORT).show();
        }
    }

    private void setData() {
        setTextOnView(myProfileName, member.getUsername());
        setTextOnView(myStatusMessage, member.getStatusMessage());
        Glide.with(this)
                .load(member.getProfileImage())
                .error(Glide.with(this)
                        .load(R.drawable.basic_profile_image)
                        .into(profileImageView))
                .into(profileImageView);
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

        changeProfileButton.setOnClickListener(view -> {
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
                    member.setProfileImage("");
                    updateMyInfo(new MemberDto(member));
                    setProfileImage(profileImageView, member.getProfileImage());
                }else {
                    Toast.makeText(this, "프로필 이미지 변경", Toast.LENGTH_SHORT).show();
                }

                return false;
            });
            popupMenu.show();
        });

        profileCloseButton.setOnClickListener(view -> finish());
    }

    private void changeMyProfileImage(Uri uri) {
        String filename = uri.getLastPathSegment();

        RequestBody fileBody = RequestBody.Companion.create(String.valueOf(uri), MediaType.parse("multipart/form-data"));
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", filename, fileBody);

        String originProfileImage = member.getProfileImage();
        uploadProfileImage(filePart, originProfileImage);

        if(!member.getProfileImage().equals(originProfileImage)) {
            updateMyInfo(new MemberDto(member));

            Glide.with(this)
                    .load(!member.getProfileImage().equals("") && member.getProfileImage() != null ? member.getProfileImage() : R.drawable.basic_profile_image)
                    .error(Glide.with(this)
                            .load(R.drawable.basic_profile_image)
                            .into(profileImageView))
                    .into(profileImageView);
        }
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
        Call<MemberDto> call = RetrofitClient.getMemberApiService().RequestUpdate(member.getUserId(), memberDto);

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

                    Log.d("내정보 업데이트 요청 : ", response.body().toString());

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
        Call<MemberDto> call = RetrofitClient.getMemberApiService().RequestUserId(memberDto);

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
                Glide.with(getApplicationContext())
                        .load(result.getProfileImage())
                        .error(Glide.with(getApplicationContext())
                                .load(R.drawable.basic_profile_image)
                                .into(profileImageView))
                        .into(profileImageView);

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

    public void uploadProfileImage(MultipartBody.Part file, String originProfileImage) {
        Call<String> call = RetrofitClient.getImageApiService().RequestUploadImage(file);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                }

                assert response.body() != null;
                String filePath = response.body();
                member.setProfileImage(filePath);

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