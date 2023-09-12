package com.example.modumessenger.Activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.modumessenger.Adapter.ChatImageSliderAdapter;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.APIHelper;
import com.example.modumessenger.Retrofit.RetrofitChatAPI;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.ChatDto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatImageActivity extends AppCompatActivity {

    List<String> chatImageList, imageChatList;

    LinearLayout chatImageLayout;
    ViewPager2 chatImageSliderViewPager;
    Button chatCloseButton;

    RetrofitChatAPI retrofitChatAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_image);

        bindingView();
        setData();
        getData();
        setEvents();
    }

    private void bindingView() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        chatImageSliderViewPager = findViewById(R.id.sliderViewPager);
        chatImageLayout = findViewById(R.id.chat_image_index_layout);
        chatCloseButton = findViewById(R.id.chat_image_close_button);
    }

    private void setData() {
        retrofitChatAPI = RetrofitClient.createChatApiService();

        imageChatList = new ArrayList<>();
    }

    private void getData() {
        chatImageList = getIntent().getStringArrayListExtra("chatImageList");

        getChatList(chatImageList);
    }

    private void setEvents() {
        chatCloseButton.setOnClickListener(v -> finish());

        chatImageSliderViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentIndex(position);
            }
        });
    }

    private void setupChatImageIndex(int count) {
    }

    private void setCurrentIndex(int position) {
        int childCount = chatImageLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) chatImageLayout.getChildAt(i);
            imageView.setImageDrawable(ContextCompat.getDrawable(this, i == position ? R.drawable.profile_image_indicator_active : R.drawable.profile_image_indicator_inactive));
        }
    }

    private void showChatImageList(List<String> imageFileList) {
        imageChatList.addAll(imageFileList);

        chatImageSliderViewPager.setOffscreenPageLimit(1);
        chatImageSliderViewPager.setAdapter(new ChatImageSliderAdapter(imageChatList));

        setupChatImageIndex(imageChatList.size());
    }

    // Retrofit function
    public void getChatList(List<String> chatImageList) {
        Call<List<ChatDto>> call = retrofitChatAPI.RequestChatList(chatImageList);

        APIHelper.enqueueWithRetry(call, 5, new Callback<List<ChatDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatDto>> call, @NonNull Response<List<ChatDto>> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null ) {
                        List<ChatDto> chatList = response.body();

                        List<String> chatImageList = chatList
                                .stream()
                                .map(ChatDto::getMessage)
                                .collect(Collectors.toList());

                        showChatImageList(chatImageList);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ChatDto>> call, @NonNull Throwable t) {
                Log.e("연결 실패", t.getMessage());
            }
        });
    }
}
