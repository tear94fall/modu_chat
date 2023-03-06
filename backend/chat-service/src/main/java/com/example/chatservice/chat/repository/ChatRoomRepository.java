package com.example.chatservice.chat.repository;

import com.example.chatservice.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomCustomRepository {

    List<ChatRoom> findAll();

    List<ChatRoom> findAllByRoomId(String roomId);

    ChatRoom findByRoomName(String roomName);

    Optional<ChatRoom> findByRoomId(String roomId);
}
