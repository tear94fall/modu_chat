package com.example.modumessenger.ViewModel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.modumessenger.Repository.ChatRepository;

public class ChatRoomListViewModelFactory implements ViewModelProvider.Factory {

    private final ChatRepository repository;

    public ChatRoomListViewModelFactory(ChatRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ChatRoomListViewModel.class)) {
            return (T) new ChatRoomListViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
