package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomCustomRepository {

    List<ChatRoom> findAll();

    List<ChatRoom> findAllByRoomId(String roomId);

    @EntityGraph(attributePaths = {"userIds"})
    ChatRoom findByRoomId(String roomId);
}
