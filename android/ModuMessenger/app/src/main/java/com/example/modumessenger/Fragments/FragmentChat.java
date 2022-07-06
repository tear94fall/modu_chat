package com.example.modumessenger.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Activity.CreateRoomActivity;
import com.example.modumessenger.Adapter.ChatRoomAdapter;
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;
import com.example.modumessenger.entity.ChatRoom;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.ChatRoomDto;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentChat extends Fragment {

    RecyclerView chatRecyclerView;
    List<ChatRoom> chatRoomList;
    FloatingActionButton chatFloatingActionButton;

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
        getChatRoomList(new Member(PreferenceManager.getString("userId"), PreferenceManager.getString("email")));
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

    private void bindingView(View view) {
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        chatRecyclerView.setHasFixedSize(true);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        chatRecyclerView.scrollToPosition(0);

        chatFloatingActionButton = view.findViewById(R.id.chatFloatingActionButton);
    }

    private void getData() {

    }

    private void setData() {
        chatRoomList = new ArrayList<>();
    }

    private void setButtonClickEvent() {
        chatFloatingActionButton.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CreateRoomActivity.class);
            v.getContext().startActivity(intent);
        });
    }

    public void showChatRoomPopupMenu(View view, ChatRoom chatRoom) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), view);
        requireActivity().getMenuInflater().inflate(R.menu.menu_chatroom_popup,popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.chat_room_exit_menu){
                Toast.makeText(requireContext(), "채팅방 나가기 :  " + chatRoom.getRoomId(), Toast.LENGTH_SHORT).show();
            }else if (menuItem.getItemId() == R.id.chat_room_enter_menu){
                Toast.makeText(requireContext(), "채팅방에 입장하기", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(requireContext(), "채팅방 클릭", Toast.LENGTH_SHORT).show();
            }

            return false;
        });

        popupMenu.show();
    }

    // Retrofit function
    public void getChatRoomList(Member member) {
        Call<List<ChatRoomDto>> call = RetrofitClient.getChatRoomApiService().RequestChatRooms(member.getUserId());

        call.enqueue(new Callback<List<ChatRoomDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatRoomDto>> call, @NonNull Response<List<ChatRoomDto>> response) {
                if (!response.isSuccessful()) {
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                assert response.body() != null;
                List<ChatRoomDto> chatRoomDtoList = response.body();

                chatRoomList.clear();

                chatRoomDtoList.forEach(c -> {
                    chatRoomList.add(new ChatRoom(c));
                });

                chatRecyclerView.setAdapter(new ChatRoomAdapter(chatRoomList, FragmentChat.this));

                Log.d("채팅방 목록 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<List<ChatRoomDto>> call, @NonNull Throwable t) {
                Log.e("-->연결실패", t.getMessage());
            }
        });
    }
}