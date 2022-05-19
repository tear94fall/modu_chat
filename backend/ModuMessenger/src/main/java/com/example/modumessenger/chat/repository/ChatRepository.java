package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long>, ChatCustomRepository {
    List<Chat> findAllByRoomId(String roomId);

    List<Chat> findAll();
}
