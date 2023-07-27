package com.example.modumessenger.Activity;

import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreMember;

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
import com.example.modumessenger.Retrofit.RetrofitProfileAPI;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.dto.ProfileDto;
import com.example.modumessenger.entity.Member;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileHistoryActivity extends AppCompatActivity {

    Long memberId;
    Member member;

    RecyclerView recyclerView;
    ProfileHistoryAdapter profileHistoryAdapter;

    RetrofitMemberAPI retrofitMemberAPI;
    RetrofitProfileAPI retrofitProfileAPI;

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
        retrofitProfileAPI = RetrofitClient.createProfileApiService();
    }

    private void getData() {
        member = getDataStoreMember();
        memberId = Long.parseLong(getIntent().getStringExtra("memberId"));

        getMemberInfo(memberId);
    }

    private void setEvents() {

    }

    // Retrofit function
    public void getMemberInfo(Long id) {
        /*
        get member info retrofit request
         */

        // finish request
        getProfileList(memberId);
    }

    // must removed
    public void getProfileHistory(String email) {
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

    public void getProfileList(Long id) {
        Call<List<ProfileDto>> call = retrofitProfileAPI.getMemberProfiles(id);

        call.enqueue(new Callback<List<ProfileDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ProfileDto>> call, @NonNull Response<List<ProfileDto>> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        List<ProfileDto> profileList = response.body();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ProfileDto>> call, @NonNull Throwable t) {

            }
        });
    }
}