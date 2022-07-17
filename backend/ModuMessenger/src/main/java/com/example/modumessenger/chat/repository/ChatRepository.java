package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long>, ChatCustomRepository {
    List<Chat> findAllByRoomId(String roomId);

    Optional<Chat> findById(Long id);

    List<Chat> findAll();

    Long countByRoomId(String roomId);
}
