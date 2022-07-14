package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.Chat;
import org.modelmapper.ModelMapper;

import java.util.List;

public interface ChatCustomRepository {

    List<Chat> findAllQueryDsl();

    Chat findByRoomIdAndChatId(String roomId, Long chatId);

    List<Chat> findByMessage(String roomId, String message);

    Long countAll();
}
