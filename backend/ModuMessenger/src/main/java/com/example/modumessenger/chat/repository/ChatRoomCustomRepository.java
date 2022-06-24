package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.Chat;
import com.example.modumessenger.chat.entity.ChatRoom;

import java.util.List;

import static com.example.modumessenger.chat.entity.QChatRoom.chatRoom;

public interface ChatRoomCustomRepository {

    List<ChatRoom> findAllQueryDsl();

    List<ChatRoom> findByUserId(String userId);

    Long countAll();
}
