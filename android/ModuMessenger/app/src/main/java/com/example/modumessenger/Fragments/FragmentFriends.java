package com.example.modumessenger.Fragments;

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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.modumessenger.Activity.FindFriendsActivity;
import com.example.modumessenger.Activity.ProfileActivity;
import com.example.modumessenger.Activity.SearchActivity;
import com.example.modumessenger.Activity.SetFriendsActivity;
import com.example.modumessenger.Adapter.FriendsAdapter;
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.MemberDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentFriends extends Fragment {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    // friend count
    TextView friendsCount;

    // my profile
    TextView myName;
    TextView myStatusMessage;
    ImageView myProfileImage;

    List<MemberDto> friendsList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("DEBUG", "onCreate of FragmentFriends");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);
        setHasOptionsMenu(true);

        // my profile
        myProfileImage = view.findViewById(R.id.myProfileImage);
        myName = view.findViewById(R.id.myName);
        myStatusMessage = view.findViewById(R.id.myStatusMessage);

        // friend count
        friendsCount = view.findViewById(R.id.friendCount);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = (RecyclerView) view.findViewById(R.id.friend_recycler_view);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.scrollToPosition(0);

        view.findViewById(R.id.myProfileCard).setOnClickListener(view1 -> {
            Intent intent = new Intent(view1.getContext(), ProfileActivity.class);

            intent.putExtra("username", PreferenceManager.getString("username"));
            intent.putExtra("statusMessage", PreferenceManager.getString("statusMessage"));
            intent.putExtra("profileImage", PreferenceManager.getString("profileImage"));

            view1.getContext().startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e("DEBUG", "onResume of FragmentFriends");

        requireActivity().invalidateOptionsMenu();

        MemberDto memberDto = new MemberDto(PreferenceManager.getString("userId"), PreferenceManager.getString("email"));

        getFriendsList(memberDto);
        getMyProfileInfo(memberDto);
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
        } else if(itemId == R.id.menu_settings) {
            clickMessage = "친구 설정";
            intent = new Intent(getContext(), SetFriendsActivity.class);
        } else if(itemId == R.id.add_friends) {
            clickMessage = "친구 추가";
            intent = new Intent(getContext(), SearchActivity.class);
        }

        if(intent!=null) {
            Toast.makeText(getActivity(), clickMessage, Toast.LENGTH_SHORT).show();
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    // Retrofit function
    public void getFriendsList(MemberDto memberDto) {
        Call<List<MemberDto>> call = RetrofitClient.getMemberApiService().RequestFriends(memberDto.getUserId());

        call.enqueue(new Callback<List<MemberDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<MemberDto>> call, @NonNull Response<List<MemberDto>> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                assert response.body() != null;
                friendsList = response.body();
                recyclerView.setAdapter(new FriendsAdapter(friendsList));

                // friend count
                String count = Integer.toString(friendsList.size());
                String friendCountMessage = "친구 " + count + " 명";
                friendsCount.setText(friendCountMessage);

                Log.d("친구 리스트 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<List<MemberDto>> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void getMyProfileInfo(MemberDto memberDto) {
        Call<MemberDto> call = RetrofitClient.getMemberApiService().RequestUserId(memberDto);

        call.enqueue(new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                MemberDto result = response.body();

                assert response.body() != null;
                assert result != null;

                // get my Profile Info
                myName.setText(result.getUsername());
                myStatusMessage.setText(result.getStatusMessage());
                Glide.with(requireContext())
                        .load(result.getProfileImage())
                        .error(Glide.with(requireContext())
                                .load(R.drawable.basic_profile_image)
                                .into(myProfileImage))
                        .into(myProfileImage);

                if(memberDto.getEmail().equals(result.getEmail())){
                    Log.d("중복검사: ", "중복된 번호가 아닙니다");
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