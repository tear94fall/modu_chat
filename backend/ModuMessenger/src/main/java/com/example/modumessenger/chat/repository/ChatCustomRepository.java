package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.Chat;

import java.util.List;

public interface ChatCustomRepository {

    List<Chat> findAllQueryDsl();

    Chat findByRoomIdAndChatId(String roomId, Long chatId);

    Long countAll();
}
