package com.example.chatservice.chat.repository;

import com.example.chatservice.chat.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long>, ChatRoomMemberCustomRepository {

    Optional<ChatRoomMember> findById(Long id);
}
