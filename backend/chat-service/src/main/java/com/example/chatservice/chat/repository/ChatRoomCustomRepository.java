package com.example.chatservice.chat.repository;

import com.example.chatservice.chat.entity.ChatRoom;

import java.util.List;

public interface ChatRoomCustomRepository {

    List<ChatRoom> findAllQueryDsl();

    Long countAll();
}
