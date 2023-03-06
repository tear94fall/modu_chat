package com.example.chatservice.chat.repository;

import com.example.chatservice.chat.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatCustomRepository {

    List<Chat> findAllQueryDsl();

    Chat findByRoomIdAndChatId(String roomId, Long chatId);

    List<Chat> findByMessage(String roomId, String message);

    List<Chat> findByRoomIdPaging(String roomId, Pageable pageable);

    List<Chat> findByRoomIdSize(String roomId, Long size);

    List<Chat> findByRoomIdAndChatId(String roomId, Long chatId, Long size);

    List<Chat> findByImageChatSize(String roomId, Long size);
}
