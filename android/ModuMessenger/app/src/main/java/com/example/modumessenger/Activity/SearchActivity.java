package com.example.modumessenger.Activity;

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
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.MemberDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {
    private static SearchActivity instance;

    SearchView searchView;
    RecyclerView findFriendRecyclerView;
    RecyclerView.LayoutManager findFriendLayoutManager;
    SearchFriendsAdapter searchFriendsAdapter;
    List<MemberDto> searchMemberList;

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
        instance = this;
        setTitle("친구 추가");

        findFriendRecyclerView = (RecyclerView) findViewById(R.id.friend_search_recycler_view);
        findFriendRecyclerView.setHasFixedSize(true);

        findFriendLayoutManager = new LinearLayoutManager(this);
        findFriendRecyclerView.setLayoutManager(findFriendLayoutManager);
        findFriendRecyclerView.scrollToPosition(0);

        searchView = findViewById(R.id.friends_search_view);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setSubmitButtonEnabled(true);
    }

    private void getData() {

    }

    private void setData() {

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

    public static SearchActivity getInstance() {
        return instance;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addFriendByEmail(MemberDto friends) {
        try {
            MemberDto memberDto = new MemberDto(PreferenceManager.getString("userId"), PreferenceManager.getString("email"));

            addFriend(memberDto, friends);
            searchFriendsAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "친구 추가에 실패헀습니다", Toast.LENGTH_SHORT).show();
        }
    }

    // Retrofit function
    public void searchFriend(String email) {
        Call<List<MemberDto>> call = RetrofitClient.getMemberApiService().RequestFriend(email);

        call.enqueue(new Callback<List<MemberDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<MemberDto>> call, @NonNull Response<List<MemberDto>> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                try {
                    assert response.body() != null;
                    Log.d("친구 검색 요청 : ", response.body().toString());
                    Toast.makeText(getApplicationContext(), email + " 을 검색하였습니다.", Toast.LENGTH_SHORT).show();

                    assert response.body() != null;
                    searchMemberList = response.body();
                    searchFriendsAdapter = new SearchFriendsAdapter(searchMemberList);
                    findFriendRecyclerView.setAdapter(searchFriendsAdapter);

                } catch (Exception e) {
                    Log.e("오류 발생 : ", e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<MemberDto>> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void addFriend(MemberDto member, MemberDto friend) {
        Call<MemberDto> call = RetrofitClient.getMemberApiService().RequestAddFriends(member.getUserId(), friend);

        call.enqueue(new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                try {
                    assert response.body() != null;
                    Log.d("친구 추가 요청 : ", response.body().toString());
                    MemberDto friend = response.body();
                    Toast.makeText(getApplicationContext(), friend.getUsername() + "님과 친구가 되었습니다.", Toast.LENGTH_SHORT).show();
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
}
