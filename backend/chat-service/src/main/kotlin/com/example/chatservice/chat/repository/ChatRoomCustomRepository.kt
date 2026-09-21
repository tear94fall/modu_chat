package com.example.chatservice.chat.repository

import com.example.chatservice.chat.entity.ChatRoom

interface ChatRoomCustomRepository {

    fun findAllQueryDsl(): List<ChatRoom>

    fun countAll(): Long

    fun findAllByMemberId(memberId: Long): List<ChatRoom>
}
