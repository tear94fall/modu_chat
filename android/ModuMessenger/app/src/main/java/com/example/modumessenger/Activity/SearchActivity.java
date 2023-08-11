package com.example.modumessenger.Activity;

import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreMember;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Adapter.SearchFriendsAdapter;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.Retrofit.RetrofitMemberAPI;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.entity.Member;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    Member member;

    SearchView searchView;
    RecyclerView findFriendRecyclerView;
    SearchFriendsAdapter searchFriendsAdapter;
    List<MemberDto> searchMemberList;
    RetrofitMemberAPI retrofitMemberAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        bindingView();
        getData();
        setData();
        setButtonClickEvent();
    }

    private void bindingView() {
        setTitle("친구 추가");

        findFriendRecyclerView = findViewById(R.id.friend_search_recycler_view);
        findFriendRecyclerView.setHasFixedSize(true);
        findFriendRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        findFriendRecyclerView.scrollToPosition(0);

        searchView = findViewById(R.id.friends_search_view);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setSubmitButtonEnabled(true);
    }

    private void getData() {
        member = getDataStoreMember();
    }

    private void setData() {
        retrofitMemberAPI = RetrofitClient.createMemberApiService();
    }

    private void setButtonClickEvent() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
            @Override
            public boolean onQueryTextSubmit(String query) {
                if(query.equals("")){
                    Toast.makeText(getApplicationContext(), "입력된 값이 없습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    searchFriend(query);
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(newText.equals("")){
                    this.onQueryTextSubmit("");
                }

                return true;
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addFriendByEmail(MemberDto friends) {
        try {
            addFriend(member.getUserId(), friends);
            searchFriendsAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "친구 추가에 실패헀습니다", Toast.LENGTH_SHORT).show();
        }
    }

    // Retrofit function
    public void searchFriend(String email) {
        Call<List<MemberDto>> call = retrofitMemberAPI.RequestFriend(email);

        call.enqueue(new Callback<List<MemberDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<MemberDto>> call, @NonNull Response<List<MemberDto>> response) {
                Log.d("친구 검색 요청 : ", response.body().toString());

                if(response.isSuccessful()) {
                    if(response.body() != null) {
                        Toast.makeText(getApplicationContext(), email + " 을 검색하였습니다.", Toast.LENGTH_SHORT).show();

                        searchMemberList = response.body();

                        searchFriendsAdapter = new SearchFriendsAdapter(searchMemberList);
                        findFriendRecyclerView.setAdapter(searchFriendsAdapter);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<MemberDto>> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void addFriend(String userId, MemberDto friend) {
        Call<MemberDto> call = retrofitMemberAPI.RequestAddFriends(userId, friend.getEmail());

        call.enqueue(new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                if(response.isSuccessful()) {
                    if(response.body() != null) {
                        Log.d("친구 추가 요청 : ", response.body().toString());
                        MemberDto memberDto = response.body();
                        Toast.makeText(getApplicationContext(), memberDto.getUsername() + "님과 친구가 되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<MemberDto> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }
}
