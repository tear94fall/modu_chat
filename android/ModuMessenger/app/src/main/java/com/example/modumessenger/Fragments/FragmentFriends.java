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
import com.example.modumessenger.Global.FriendSort;
import com.example.modumessenger.Global.FriendsPager;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.dto.PageResponseDto;
import com.example.modumessenger.entity.Member;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentFriends extends Fragment {

    private static final int FRIENDS_PAGE_SIZE = 50;
    /** 끝에서 이만큼 남았을 때 다음 페이지를 미리 읽는다. */
    private static final int LOAD_MORE_THRESHOLD = 5;

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    ConstraintLayout myProfileCard;
    TextView friendsCount, myName, myStatusMessage;
    ImageView myProfileImage;

    List<MemberDto> friendsList;
    FriendsAdapter friendsAdapter;
    FriendsPager friendsPager;
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

        // 탭에 돌아올 때마다 처음부터 다시 읽는다. 진행 중이던 요청의 늦은 응답은 pager 가 세대로 걸러 낸다.
        friendsPager.reset();
        friendsAdapter.clear();
        loadNextFriendsPage();
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

        friendsList = new ArrayList<>();
        friendsAdapter = new FriendsAdapter(friendsList);
        recyclerView.setAdapter(friendsAdapter);
        friendsPager = new FriendsPager(FRIENDS_PAGE_SIZE);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return;
                int lastVisible = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
                if (lastVisible >= friendsAdapter.getItemCount() - LOAD_MORE_THRESHOLD) {
                    loadNextFriendsPage();
                }
            }
        });
    }

    private void setButtonClickEvent() {
        myProfileCard.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), ProfileActivity.class);
            intent.putExtra("memberId", String.valueOf(member.getId()));

            view.getContext().startActivity(intent);
        });
    }

    // Retrofit function
    private void loadNextFriendsPage() {
        if (!friendsPager.canLoad()) return;

        int page = friendsPager.beginLoad();
        int generation = friendsPager.getGeneration();
        Call<PageResponseDto<MemberDto>> call = retrofitMemberAPI.RequestFriends(member.getUserId(), FriendSort.DEFAULT, page, friendsPager.getPageSize());

        call.enqueue(new Callback<PageResponseDto<MemberDto>>() {
            @Override
            public void onResponse(@NonNull Call<PageResponseDto<MemberDto>> call, @NonNull Response<PageResponseDto<MemberDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    friendsPager.onFailed();
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                PageResponseDto<MemberDto> body = response.body();
                if (!friendsPager.onLoaded(generation, body)) {
                    return; // 초기화 이전에 보낸 요청의 응답. 지금 목록과 맞지 않으므로 버린다.
                }

                friendsAdapter.addAll(body.getContent());
                String friendCountMessage = "친구 " + body.getTotalElements() + " 명";
                friendsCount.setText(friendCountMessage);

                Log.d("친구 리스트 가져오기 요청 : ", "page " + body.getPage() + " / " + body.getTotalPages());
            }

            @Override
            public void onFailure(@NonNull Call<PageResponseDto<MemberDto>> call, @NonNull Throwable t) {
                friendsPager.onFailed();
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

                        String statusMessage = memberDto.getStatusMessage().length() > 15 ? memberDto.getStatusMessage().substring(0, 12) + "..." : memberDto.getStatusMessage();
                        myStatusMessage.setText(statusMessage);

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