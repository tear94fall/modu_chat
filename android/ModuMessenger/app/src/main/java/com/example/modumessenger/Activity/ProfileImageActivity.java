package com.example.modumessenger.Activity;

import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreMember;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.modumessenger.Adapter.ProfileImageSliderAdapter;
import com.example.modumessenger.Global.DataStoreHelper;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.Retrofit.RetrofitMemberAPI;
import com.example.modumessenger.Retrofit.RetrofitProfileAPI;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.dto.ProfileDto;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.entity.ProfileType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileImageActivity extends AppCompatActivity {

    ProfileType type;
    Long memberId, profileId;
    String email;
    Member member;

    LinearLayout profileImageLayout;
    ViewPager2 profileImageSliderViewPager;
    Button profileCloseButton;
    ImageButton profileDownloadButton, profileDeleteButton;
    List<String> profileImageList;

    Disposable backgroundTask;

    RetrofitMemberAPI retrofitMemberAPI;
    RetrofitProfileAPI retrofitProfileAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_image);

        bindingView();
        setData();
        getData();
        setEvents();
    }

    private void bindingView() {
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).hide();

        profileImageSliderViewPager = findViewById(R.id.sliderViewPager);
        profileImageLayout = findViewById(R.id.profile_image_index_layout);
        profileCloseButton = findViewById(R.id.profile_image_close_button);
        profileDownloadButton = findViewById(R.id.profile_image_download_button);
        profileDeleteButton = findViewById(R.id.profile_image_delete_button);
    }

    private void setData() {
        retrofitMemberAPI = RetrofitClient.createMemberApiService();
        retrofitProfileAPI = RetrofitClient.createProfileApiService();

        profileImageList = new ArrayList<>();
    }

    private void getData() {
        member = getDataStoreMember();
        type = Enum.valueOf(ProfileType.class, getIntent().getStringExtra("type"));
        memberId = Long.parseLong(getIntent().getStringExtra("memberId"));

        if (getIntent().getStringExtra("profileId").equals("")) {
            getMemberProfileList(memberId);
            getMyProfileInfo(email);
        } else {
            profileId = Long.parseLong(getIntent().getStringExtra("profileId"));
            getMemberProfile(memberId, profileId);
        }
    }

    private void setEvents() {
        profileCloseButton.setOnClickListener(v -> finish());

        profileDownloadButton.setOnClickListener(v -> {
            int currentItem = profileImageSliderViewPager.getCurrentItem();
            String imageFile = this.profileImageList.get(currentItem);
            saveImageFromUrl(imageFile);
        });

        profileDeleteButton.setOnClickListener(v -> {
            int currentItem = profileImageSliderViewPager.getCurrentItem();
            String imageFile = this.profileImageList.get(currentItem);
            deleteProfileImage(member.getUserId(), imageFile);
        });

        profileImageSliderViewPager.registerOnPageChangeCallback(new OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentIndex(position);
            }
        });
    }

    private void setupProfileImageIndex(int count) {
        List<ImageView> profileImageViewList = Arrays.asList(new ImageView[count]);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(16, 8, 16, 8);

        profileImageViewList.forEach(profileImageView -> {
            profileImageView = new ImageView(this);
            profileImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.profile_image_indicator_inactive));
            profileImageView.setLayoutParams(params);
            profileImageLayout.addView(profileImageView);
        });

        setCurrentIndex(0);
    }

    private void setCurrentIndex(int position) {
        int childCount = profileImageLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) profileImageLayout.getChildAt(i);
            imageView.setImageDrawable(ContextCompat.getDrawable(this, i == position ? R.drawable.profile_image_indicator_active : R.drawable.profile_image_indicator_inactive));
        }
    }

    private void showImageLists(List<String> imageFileList) {
        profileImageList.addAll(imageFileList);

        profileImageSliderViewPager.setOffscreenPageLimit(1);
        profileImageSliderViewPager.setAdapter(new ProfileImageSliderAdapter(profileImageList));

        setupProfileImageIndex(profileImageList.size());
    }

    public void saveFile(@NonNull final File file, @NonNull final String mimeType, @NonNull final String displayName) throws IOException {
        // check permission
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "사진 다운로드를 취소 합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 1000);

            return;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) values.put(MediaStore.Images.Media.IS_PENDING, 1);

        ContentResolver contentResolver = getContentResolver();
        Uri collection = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q ? MediaStore.Downloads.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Uri item = contentResolver.insert(collection, values);

        try {
            OutputStream stream = contentResolver.openOutputStream(item);
            if(stream == null) {
                throw new IOException("Failed to open output stream.");
            }

            byte[] bArray = getByteArrayFromFile(file);
            stream.write(bArray);
            stream.flush();
            stream.close();

            values.clear();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) values.put(MediaStore.Images.Media.IS_PENDING, 0);

            contentResolver.update(item, values, null, null);
            Toast.makeText(this, "사진이 저장 되었습니다.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "사진을 저장할 수 없습니다.", Toast.LENGTH_SHORT).show();
            if (item != null) {
                contentResolver.delete(item, null, null);
            }
            if(!backgroundTask.isDisposed()) {
                backgroundTask.dispose();
            }
            throw e;
        }
    }

    private byte[] getByteArrayFromFile(File file){
        FileInputStream fis = null;
        byte[] buffer = new byte[(int) file.length()];

        try{
            fis = new FileInputStream(file);
            if (fis.read(buffer) < 0)
                throw new IOException("file read error");
        }catch(IOException ioExp){
            ioExp.printStackTrace();
        }finally{
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return buffer;
    }

    private void saveImageFromUrl(String imageFile) {
        String accessToken = DataStoreHelper.getDataStoreStr("access-token");
        String url = RetrofitClient.getBaseUrl() + "storage-service/view/"+ imageFile;

        GlideUrl glideUrl = new GlideUrl(url,
                new LazyHeaders.Builder()
                        .addHeader("Authorization", accessToken)
                        .build());

        backgroundTask = Observable.fromCallable(() ->
                Glide.with(this).asFile().load(glideUrl).submit().get())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(file -> saveFile(file, imageFile, imageFile));
    }

    private String extractImageFileName(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
    }

    private String extractImageExtType(String imageUrl) {
        String fileExt = imageUrl.substring(imageUrl.lastIndexOf(".") + 1);
        return fileExt.equalsIgnoreCase("jpg") ? "image/jpeg" : "image/" + fileExt.toLowerCase();
    }

    private List<String> getProfileListByType(ProfileType type, List<ProfileDto> profileList) {
        List<String> list = profileList.stream()
                .filter(profile -> {
                    if (type == ProfileType.PROFILE_IMAGE) {
                        return profile.getProfileType().equals(ProfileType.PROFILE_IMAGE);
                    } else if (type == ProfileType.PROFILE_WALLPAPER) {
                        return profile.getProfileType().equals(ProfileType.PROFILE_WALLPAPER);
                    } else if (type == ProfileType.PROFILE_STATUS_MESSAGE) {
                        return false;
                    }

                    return false;
                })
                .sorted(Comparator.comparing(ProfileDto::getLastModifiedDateTime).reversed())
                .map(ProfileDto::getValue)
                .collect(Collectors.toList());

        if (list.size() == 0) {
            if (type == ProfileType.PROFILE_IMAGE) {
                list = new ArrayList<>(Collections.singletonList(member.getProfileImage()));
            } else if (type == ProfileType.PROFILE_WALLPAPER) {
                list = new ArrayList<>(Collections.singletonList(member.getWallpaperImage()));
            }
        }

        return list;
    }

    // Retrofit function
    public void getMemberProfile(Long memberId, Long profileId) {
        Call<ProfileDto> call = retrofitProfileAPI.RequestMemberProfile(Long.toString(memberId), Long.toString(profileId));

        call.enqueue(new Callback<ProfileDto>() {
            @Override
            public void onResponse(@NonNull Call<ProfileDto> call, @NonNull Response<ProfileDto> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        ProfileDto profileDto = response.body();

                        List<String> profileImageList = getProfileListByType(type, new ArrayList<>(Collections.singletonList(profileDto)));
                        showImageLists(profileImageList);
                    }
                }

                Log.d("프로필 리스트 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<ProfileDto> call, @NonNull Throwable t) {
                Log.e("연결 실패", t.getMessage());
            }
        });
    }

    public void getMemberProfileList(Long memberId) {
        Call<List<ProfileDto>> call = retrofitProfileAPI.RequestMemberProfiles(memberId);

        call.enqueue(new Callback<List<ProfileDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ProfileDto>> call, @NonNull Response<List<ProfileDto>> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        List<ProfileDto> profileList = response.body();

                        List<String> profileImageList = getProfileListByType(type, profileList);
                        showImageLists(profileImageList);
                    }
                }

                Log.d("프로필 리스트 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<List<ProfileDto>> call, @NonNull Throwable t) {
                Log.e("연결 실패", t.getMessage());
            }
        });
    }

    public void getMyProfileInfo(String email) {
        Call<MemberDto> call = retrofitMemberAPI.RequestUserInfo(email);

        call.enqueue(new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        MemberDto memberDto = response.body();

                        if (!memberDto.getUserId().equals(member.getUserId())) {
                            profileDeleteButton.setVisibility(View.GONE);
                        }
                    }
                }

                Log.d("내 정보 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<MemberDto> call, @NonNull Throwable t) {
                Log.e("연결 실패", t.getMessage());
            }
        });
    }

    public void deleteProfileImage(String userId, String image) {
        Call<MemberDto> call = retrofitMemberAPI.RequestDeleteProfileImage(userId, image);

        call.enqueue(new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        profileImageList.stream()
                                .filter(p -> p.equals(image))
                                .findFirst().ifPresent(s -> profileImageList.remove(s));
                    }
                }

                Log.d("프로필 이미지 삭제 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<MemberDto> call, @NonNull Throwable t) {
                Log.e("연결 실패", t.getMessage());
            }
        });
    }

    public void getProfileCount(String memberId) {
        Call<Long> call = retrofitProfileAPI.RequestTotalProfileCount(memberId);

        call.enqueue(new Callback<Long>() {
            @Override
            public void onResponse(@NonNull Call<Long> call, @NonNull Response<Long> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        Long deleteId = response.body();

                        Toast.makeText(getApplicationContext(), "프로필 삭제 완료", Toast.LENGTH_LONG).show();

                        Log.d(String.format("프로필 갯수 조회 요청  (회원 id: %s)", memberId), deleteId.toString());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Long> call, @NonNull Throwable t) {
                Log.e("연결 실패", t.getMessage());
            }
        });
    }
}
