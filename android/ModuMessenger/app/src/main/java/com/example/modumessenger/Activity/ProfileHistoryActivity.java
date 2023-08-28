package com.example.modumessenger.Activity;

import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreMember;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
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
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.entity.Profile;

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
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

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

        getMemberInfoById(member.getId(), memberId);
    }

    private void setEvents() {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void getMemberInfoById(Long myId, Long id) {
        getProfileHistory(myId, id);
    }

    public void OpenProfilePopupMenu(View view, Profile profile) {
        PopupMenu popup = new PopupMenu(ProfileHistoryActivity.this, view);
        getMenuInflater().inflate(R.menu.menu_profile_list, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.set_profile:
                    Toast.makeText(getApplicationContext(), "프로필로 설정 하기: " + profile.getId(), Toast.LENGTH_LONG).show();
                    break;

                case R.id.save_profile:
                    Toast.makeText(getApplicationContext(), "프로필 저장 하기: " + profile.getId(), Toast.LENGTH_LONG).show();
                    break;

                case R.id.delete_profile:
                    deleteProfile(profile);
                    break;

                default:
                    break;
            }
            return false;
        });

        popup.show();
    }

    // Retrofit function
    public void getProfileHistory(Long myId, Long id) {
        Call<MemberDto> call = retrofitMemberAPI.RequestMemberById(id);

        call.enqueue(new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        MemberDto memberDto = response.body();
                        Member member = new Member(memberDto);

                        setTitle(member.getUsername());

                        profileHistoryAdapter = new ProfileHistoryAdapter(member, myId.equals(id));
                        profileHistoryAdapter.setProfileMenuClickListener((view, profile) -> OpenProfilePopupMenu(view, profile));

                        recyclerView.setAdapter(profileHistoryAdapter);
                    }

                    Log.d("프로필 리스트 가져 오기 요청 id : ", response.body().getId().toString());
                }
            }

            @Override
            public void onFailure(@NonNull Call<MemberDto> call, @NonNull Throwable t) {
                Log.e("연결 실패", t.getMessage());
            }
        });
    }

    public void deleteProfile(Profile profile) {
        String memberId = Long.toString(profile.getMemberId());
        String id = Long.toString(profile.getId());

        Call<Long> call = retrofitProfileAPI.RequestDeleteProfile(memberId, id);

        call.enqueue(new Callback<Long>() {
            @Override
            public void onResponse(@NonNull Call<Long> call, @NonNull Response<Long> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        Long deleteId = response.body();

                        Toast.makeText(getApplicationContext(), "프로필 삭제 완료", Toast.LENGTH_LONG).show();
                        Log.d(String.format("프로필 삭제 하기 요청 (회원 id: %s, 프로필 id: %s)", memberId, id), deleteId.toString());
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