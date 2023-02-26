package com.example.modumessenger.Activity;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.Retrofit.RetrofitMemberAPI;
import com.example.modumessenger.dto.MemberDto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileImageActivity  extends AppCompatActivity {

    LinearLayout profileImageLayout;
    ViewPager2 profileImageSliderViewPager;
    Button profileCloseButton;
    ImageButton profileDownloadButton;
    List<String> profileImageList;
    String userId, email;

    Disposable backgroundTask;

    RetrofitMemberAPI retrofitMemberAPI;

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
    }

    private void setData() {
        retrofitMemberAPI = RetrofitClient.createMemberApiService();

        profileImageList = new ArrayList<>();
    }

    private void getData() {
        userId = getIntent().getStringExtra("userId");
        email = getIntent().getStringExtra("email");

        ArrayList<String> imageFileList = getIntent().getStringArrayListExtra("imageFileList");

        if(imageFileList == null || imageFileList.size() == 0) {
            getMyProfileInfo(new MemberDto(userId, email));
        } else {
            showImageLists(imageFileList);
        }
    }

    private void setEvents() {
        profileCloseButton.setOnClickListener(v -> finish());

        profileDownloadButton.setOnClickListener(v -> {
            int currentItem = profileImageSliderViewPager.getCurrentItem();
            String imageFile = this.profileImageList.get(currentItem);
            saveImageFromUrl(imageFile);
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

    private void showImageLists(ArrayList<String> imageFileList) {
        profileImageList.addAll(imageFileList);

        profileImageSliderViewPager.setOffscreenPageLimit(1);
        profileImageSliderViewPager.setAdapter(new ProfileImageSliderAdapter(profileImageList));

        setupProfileImageIndex(profileImageList.size());
    }

    public void saveFile(@NonNull final File file, @NonNull final String mimeType, @NonNull final String displayName) throws IOException {
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
            Toast.makeText(this, "사진이 저장되었습니다.", Toast.LENGTH_SHORT).show();
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
        String accessToken = PreferenceManager.getString("access-token");
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

    // Retrofit function
    public void getMyProfileInfo(MemberDto memberDto) {
        Call<MemberDto> call = retrofitMemberAPI.RequestUserInfo(memberDto.getEmail());

        call.enqueue(new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                assert response.body() != null;
                MemberDto result = response.body();

                profileImageList.add(result.getProfileImage());

                profileImageSliderViewPager.setOffscreenPageLimit(1);
                profileImageSliderViewPager.setAdapter(new ProfileImageSliderAdapter(profileImageList));

                setupProfileImageIndex(profileImageList.size());

                Log.d("내 정보 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<MemberDto> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }
}
