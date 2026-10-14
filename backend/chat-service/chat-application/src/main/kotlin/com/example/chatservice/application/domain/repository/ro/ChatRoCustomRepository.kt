package com.example.chatservice.application.domain.repository.ro

import com.example.chatservice.application.domain.entity.Chat
import org.springframework.data.domain.Pageable

interface ChatRoCustomRepository {

    fun findByMessage(roomId: String, message: String): List<Chat>

    fun findByRoomIdPaging(roomId: String, pageable: Pageable): List<Chat>

    fun findAllByRoomId(roomId: String, blockedSenders: Collection<String>): List<Chat>

    fun findByRoomIdSize(roomId: String, size: Long, blockedSenders: Collection<String>): List<Chat>

    fun findByRoomIdAndChatId(roomId: String, chatId: Long, size: Long, blockedSenders: Collection<String>): List<Chat>

    fun findByImageChatSize(roomId: String, size: Long, blockedSenders: Collection<String>): List<Chat>
}
