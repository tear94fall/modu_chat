package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.ChatRoom;

import java.util.List;

public interface ChatRoomCustomRepository {

    public List<ChatRoom> findAllById(Long id);
}
