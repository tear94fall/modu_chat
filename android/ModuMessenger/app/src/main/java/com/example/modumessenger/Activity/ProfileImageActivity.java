package com.example.modumessenger.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import com.example.modumessenger.Adapter.ProfileImageSliderAdapter;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.MemberDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
        profileImageList = new ArrayList<>();
    }

    private void getData() {
        userId = getIntent().getStringExtra("userId");
        email = getIntent().getStringExtra("email");

        ArrayList<String> imageUrlList = getIntent().getStringArrayListExtra("imageUrlList");

        if(imageUrlList == null || imageUrlList.size() == 0) {
            getMyProfileInfo(new MemberDto(userId, email));
        } else {
            showImageLists(imageUrlList);
        }
    }

    private void setEvents() {
        profileCloseButton.setOnClickListener(v -> finish());

        profileDownloadButton.setOnClickListener(v -> {
            int currentItem = profileImageSliderViewPager.getCurrentItem();
            String imageUrl = this.profileImageList.get(currentItem);
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

    private void showImageLists(ArrayList<String> imageUrlList) {
        profileImageList.addAll(imageUrlList);

        profileImageSliderViewPager.setOffscreenPageLimit(1);
        profileImageSliderViewPager.setAdapter(new ProfileImageSliderAdapter(profileImageList));

        setupProfileImageIndex(profileImageList.size());
    }

    // Retrofit function
    public void getMyProfileInfo(MemberDto memberDto) {
        Call<MemberDto> call = RetrofitClient.getMemberApiService().RequestUserId(memberDto);

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
