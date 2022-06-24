package com.example.modumessenger.Activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Adapter.FindFriendsAdapter;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.MemberDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FindFriendsActivity extends AppCompatActivity {

    SearchView searchView;
    RecyclerView findFriendRecyclerView;
    List<MemberDto> findFriendList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_friends);

        bindingView();
        getData();
        setData();
        setButtonClickEvent();
    }

    private void bindingView() {
        setTitle("친구 찾기");

        findFriendRecyclerView = (RecyclerView) findViewById(R.id.friend_find_recycler_view);
        findFriendRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        findFriendRecyclerView.setHasFixedSize(true);
        findFriendRecyclerView.scrollToPosition(0);

        searchView = findViewById(R.id.friends_find_view);
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

    // Retrofit function
    public void searchFriend(String email) {
        Call<List<MemberDto>> call = RetrofitClient.getMemberApiService().RequestFriend(email);
        // get member list by email
        // need to add, find from my friends by email

        call.enqueue(new Callback<List<MemberDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<MemberDto>> call, @NonNull Response<List<MemberDto>> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                try {
                    Log.d("친구 검색 요청 : ", response.body().toString());
                    Toast.makeText(getApplicationContext(), email + " 을 검색하였습니다.", Toast.LENGTH_SHORT).show();

                    assert response.body() != null;
                    findFriendList = response.body();
                    findFriendRecyclerView.setAdapter(new FindFriendsAdapter(findFriendList));

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
}