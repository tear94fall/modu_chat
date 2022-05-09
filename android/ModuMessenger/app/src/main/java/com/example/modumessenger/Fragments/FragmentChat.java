package com.example.modumessenger.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Activity.ChatActivity;
import com.example.modumessenger.Adapter.ChatRoomAdapter;
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.Member;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.ChatRoomDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentChat extends Fragment {

    RecyclerView chatRecyclerView;
    RecyclerView.LayoutManager chatLayoutManager;

    List<ChatRoomDto> chatRoomList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        chatRecyclerView.setHasFixedSize(true);

        chatLayoutManager = new LinearLayoutManager(getActivity());
        chatRecyclerView.setLayoutManager(chatLayoutManager);
        chatRecyclerView.scrollToPosition(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e("DEBUG", "onResume of FragmentFriends");

        requireActivity().invalidateOptionsMenu();

        Member member = new Member(PreferenceManager.getString("userId"), PreferenceManager.getString("email"));

        getChatRoomList(member);
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

    // Retrofit function
    public void getChatRoomList(Member member ) {
        Call<List<ChatRoomDto>> call = RetrofitClient.getChatApiService().RequestChatRooms(member.getUserId());

        call.enqueue(new Callback<List<ChatRoomDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatRoomDto>> call, @NonNull Response<List<ChatRoomDto>> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                assert response.body() != null;
                chatRoomList = response.body();
                chatRecyclerView.setAdapter(new ChatRoomAdapter(chatRoomList));

                Log.d("채팅방 목록 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<List<ChatRoomDto>> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }
}