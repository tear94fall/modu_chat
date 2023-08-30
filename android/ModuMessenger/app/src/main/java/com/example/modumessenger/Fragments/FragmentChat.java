package com.example.modumessenger.Fragments;

import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreMember;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Activity.CreateRoomActivity;
import com.example.modumessenger.Adapter.ChatRoomAdapter;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitChatRoomAPI;
import com.example.modumessenger.RoomDatabase.Database.ChatRoomDatabase;
import com.example.modumessenger.RoomDatabase.Entity.ChatRoomEntity;
import com.example.modumessenger.entity.ChatRoom;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.ChatRoomDto;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentChat extends Fragment {

    RecyclerView chatRecyclerView;
    List<ChatRoom> chatRoomList;
    FloatingActionButton chatFloatingActionButton;

    ChatRoomDatabase chatRoomDatabase;
    RetrofitChatRoomAPI retrofitChatRoomAPI;

    Member member;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        chatRoomDatabase = ChatRoomDatabase.getInstance(getActivity());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
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
        getChatRoomList(member.getId());
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
        inflater.inflate(R.menu.menu_chatroom_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        String clickMessage = "";

        if(itemId == R.id.chatroom_search) {
            clickMessage = "채팅방 찾기";
        } else if(itemId == R.id.chatroom_settings) {
            clickMessage = "채팅방 설정";
        }

        Toast.makeText(requireContext(), clickMessage, Toast.LENGTH_SHORT).show();

        return super.onOptionsItemSelected(item);
    }

    private void bindingView(View view) {
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        chatRecyclerView.setHasFixedSize(true);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        chatRecyclerView.scrollToPosition(0);

        chatFloatingActionButton = view.findViewById(R.id.chatFloatingActionButton);
    }

    private void getData() {
        member = getDataStoreMember();
    }

    private void setData() {
        retrofitChatRoomAPI = RetrofitClient.createChatRoomApiService();

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
    public void getChatRoomList(Long memberId) {
        Call<List<ChatRoomDto>> call = retrofitChatRoomAPI.RequestChatRooms(Long.toString(memberId));

        call.enqueue(new Callback<List<ChatRoomDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatRoomDto>> call, @NonNull Response<List<ChatRoomDto>> response) {
                if(response.isSuccessful()) {
                    if(response.body() != null) {
                        List<ChatRoomDto> chatRoomDtoList = response.body();

                        if(chatRoomList.size() != chatRoomDtoList.size()) {
                            chatRoomList.clear();

                            chatRoomDtoList.forEach(c -> {
                                chatRoomList.add(new ChatRoom(c));
                            });

                            chatRecyclerView.setAdapter(new ChatRoomAdapter(chatRoomList, FragmentChat.this));
                        }

                        Log.d("채팅방 목록 가져오기 요청 : ", response.body().toString());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ChatRoomDto>> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());

                LiveData<List<ChatRoomEntity>> all = chatRoomDatabase.chatRoomDao().getAll();

                all.observe(requireActivity(), chatRoomEntities -> {
                    List<ChatRoom> collect = chatRoomEntities.stream().map(ChatRoom::new).collect(Collectors.toList());
                    chatRecyclerView.setAdapter(new ChatRoomAdapter(collect, FragmentChat.this));
                });
            }
        });
    }

    public void searchChatRoomName(String roomName) {
        Call<List<ChatRoomDto>> call = retrofitChatRoomAPI.RequestSearchChatRooms(roomName);

        call.enqueue(new Callback<List<ChatRoomDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatRoomDto>> call, @NonNull Response<List<ChatRoomDto>> response) {
                if(response.isSuccessful()) {
                    if(response.body() != null) {
                        List<ChatRoomDto> chatRoomDtoList = response.body();
                        Log.d("채팅방 검색 : ", response.body().toString());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ChatRoomDto>> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }
}