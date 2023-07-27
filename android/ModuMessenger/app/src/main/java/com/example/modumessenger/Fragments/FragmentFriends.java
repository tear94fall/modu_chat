package com.example.modumessenger.Fragments;

import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreMember;
import static com.example.modumessenger.Global.GlideUtil.setProfileImage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Activity.FindFriendsActivity;
import com.example.modumessenger.Activity.ProfileActivity;
import com.example.modumessenger.Activity.SearchActivity;
import com.example.modumessenger.Activity.SetFriendsActivity;
import com.example.modumessenger.Adapter.FriendsAdapter;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.Retrofit.RetrofitMemberAPI;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.entity.Member;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentFriends extends Fragment {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    ConstraintLayout myProfileCard;
    TextView friendsCount, myName, myStatusMessage;
    ImageView myProfileImage;

    List<MemberDto> friendsList;
    Member member;

    RetrofitMemberAPI retrofitMemberAPI;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("DEBUG", "onCreate of FragmentFriends");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);
        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindingView(view);
        getData();
        setData();
        setButtonClickEvent();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e("DEBUG", "onResume of FragmentFriends");

        requireActivity().invalidateOptionsMenu();

        getFriendsList(member.getUserId());
        getMyProfileInfo(member.getEmail());
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e("DEBUG", "onPause of FragmentFriends");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e("DEBUG", "onStop of FragmentFriends");
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_friends_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        Intent intent = null;
        String clickMessage = "";

        if(itemId == R.id.menu_search) {
            clickMessage = "친구 찾기";
            intent = new Intent(getContext(), FindFriendsActivity.class);
        } else if(itemId == R.id.add_friends) {
            clickMessage = "친구 추가";
            intent = new Intent(getContext(), SearchActivity.class);
        }else if(itemId == R.id.menu_settings) {
            clickMessage = "친구 설정";
            intent = new Intent(getContext(), SetFriendsActivity.class);
        }

        if(intent!=null) {
            Toast.makeText(getActivity(), clickMessage, Toast.LENGTH_SHORT).show();
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    private void bindingView(View view) {
        myProfileCard = view.findViewById(R.id.myProfileCard);
        myProfileImage = view.findViewById(R.id.myProfileImage);
        myName = view.findViewById(R.id.myName);
        myStatusMessage = view.findViewById(R.id.myStatusMessage);
        friendsCount = view.findViewById(R.id.friendCount);

        recyclerView = (RecyclerView) view.findViewById(R.id.friend_recycler_view);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.scrollToPosition(0);
    }

    private void getData() {
        member = getDataStoreMember();
    }

    private void setData() {
        retrofitMemberAPI = RetrofitClient.createMemberApiService();
    }

    private void setButtonClickEvent() {
        myProfileCard.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), ProfileActivity.class);
            intent.putExtra("memberId", String.valueOf(member.getId()));
            intent.putExtra("email", member.getEmail());
            intent.putExtra("userId", member.getUserId());

            view.getContext().startActivity(intent);
        });
    }

    // Retrofit function
    public void getFriendsList(String userId) {
        Call<List<MemberDto>> call = retrofitMemberAPI.RequestFriends(userId);

        call.enqueue(new Callback<List<MemberDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<MemberDto>> call, @NonNull Response<List<MemberDto>> response) {
                if(response.isSuccessful()) {
                    if(response.body() != null) {
                        friendsList = response.body();
                        recyclerView.setAdapter(new FriendsAdapter(friendsList));

                        // friend count
                        String count = Integer.toString(friendsList.size());
                        String friendCountMessage = "친구 " + count + " 명";
                        friendsCount.setText(friendCountMessage);

                        Log.d("친구 리스트 가져오기 요청 : ", response.body().toString());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<MemberDto>> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void getMyProfileInfo(String email) {
        Call<MemberDto> call = retrofitMemberAPI.RequestUserInfo(email);

        call.enqueue(new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                if(response.isSuccessful()) {
                    if(response.body() != null) {
                        MemberDto memberDto = response.body();

                        myName.setText(memberDto.getUsername());
                        myStatusMessage.setText(memberDto.getStatusMessage());
                        setProfileImage(myProfileImage, memberDto.getProfileImage());

                        if(email.equals(memberDto.getEmail())){
                            Log.d("중복 검사: ", "중복된 번호가 아닙니다.");
                        }

                        Log.d("내 정보 가져 오기 요청 : ", response.body().toString());
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