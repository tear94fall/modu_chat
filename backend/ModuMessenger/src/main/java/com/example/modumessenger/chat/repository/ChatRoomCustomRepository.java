package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.Chat;
import com.example.modumessenger.chat.entity.ChatRoom;

import java.util.List;

public interface ChatRoomCustomRepository {

    List<ChatRoom> findAllQueryDsl();

    List<ChatRoom> findAllById(String userId);

    Long countAll();
}
