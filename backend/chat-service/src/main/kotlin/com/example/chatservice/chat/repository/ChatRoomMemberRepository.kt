package com.example.chatservice.chat.repository

import com.example.chatservice.chat.entity.ChatRoomMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatRoomMemberRepository : JpaRepository<ChatRoomMember, Long>, ChatRoomMemberCustomRepository
