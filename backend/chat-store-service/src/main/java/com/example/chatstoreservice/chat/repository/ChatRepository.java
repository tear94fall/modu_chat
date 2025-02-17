package com.example.chatstoreservice.chat.repository;

import com.example.chatstoreservice.chat.entity.Chat;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRepository extends MongoRepository<Chat, String> {

    Optional<Chat> findByChatId(Long chatId);
}
