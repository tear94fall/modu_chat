package com.example.modumessenger.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.modumessenger.Repository.ChatRepository;
import com.example.modumessenger.entity.ChatRoom;

import java.util.List;

public class ChatRoomListViewModel extends ViewModel {

    private final ChatRepository repository;

    public ChatRoomListViewModel(ChatRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<ChatRoom>> getChatRooms() {
        return repository.getChatRooms();
    }

    public void refresh() {
        repository.refreshChatRooms();
    }
}
