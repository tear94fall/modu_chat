package com.example.modumessenger.Activity;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Adapter.ProfileHistoryAdapter;
import com.example.modumessenger.Global.RecyclerDecorationHeight;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.Retrofit.RetrofitMemberAPI;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.entity.Member;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileHistoryActivity extends AppCompatActivity {

    String email;

    RecyclerView recyclerView;
    ProfileHistoryAdapter profileHistoryAdapter;

    RetrofitMemberAPI retrofitMemberAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_image_history);

        bindingView();
        setData();
        getData();
        setEvents();
    }

    private void bindingView() {
        recyclerView = findViewById(R.id.profile_history_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.scrollToPosition(0);
        recyclerView.addItemDecoration(new RecyclerDecorationHeight(30));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), new LinearLayoutManager(this).getOrientation()));
    }

    private void setData() {
        retrofitMemberAPI = RetrofitClient.createMemberApiService();
    }

    private void getData() {
        email = getIntent().getStringExtra("email");
        setProfileHistory(email);
    }

    private void setEvents() {

    }

    // Retrofit function
    public void setProfileHistory(String email) {
        Call<MemberDto> call = retrofitMemberAPI.RequestUserInfo(email);

        call.enqueue(new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        MemberDto memberDto = response.body();
                        Member member = new Member(memberDto);

                        setTitle(member.getUsername());

                        profileHistoryAdapter = new ProfileHistoryAdapter(member);
                        recyclerView.setAdapter(profileHistoryAdapter);
                    }
                }

                Log.d("내 정보 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<MemberDto> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }
}