package com.example.modumessenger.ViewModel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.modumessenger.Repository.ChatRepository;

public class ChatViewModelFactory implements ViewModelProvider.Factory {

    private final ChatRepository repository;
    private final String roomId;
    private final String myUserId;

    public ChatViewModelFactory(ChatRepository repository, String roomId, String myUserId) {
        this.repository = repository;
        this.roomId = roomId;
        this.myUserId = myUserId;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ChatViewModel.class)) {
            return (T) new ChatViewModel(repository, roomId, myUserId);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
