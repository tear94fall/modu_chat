package com.example.chatservice.application.domain.repository.ro

import com.example.chatservice.application.domain.entity.ChatRoom

interface ChatRoomRoCustomRepository {

    fun findAllQueryDsl(): List<ChatRoom>

    fun countAll(): Long

    fun findAllByMemberId(memberId: Long): List<ChatRoom>
}
