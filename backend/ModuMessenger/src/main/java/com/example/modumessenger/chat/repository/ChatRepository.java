package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long>, ChatCustomRepository {
    List<Chat> findAllByRoomId(String roomId);
}
