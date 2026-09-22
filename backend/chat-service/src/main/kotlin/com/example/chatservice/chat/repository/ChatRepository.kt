package com.example.chatservice.chat.repository

import com.example.chatservice.chat.entity.Chat
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatRepository : JpaRepository<Chat, Long>, ChatCustomRepository {

    fun findAllByIdIn(ids: List<Long>): List<Chat>

    fun findAllByRoomId(roomId: String): List<Chat>

    fun findByRoomId(roomId: String, pageable: Pageable): Page<Chat>

    fun countByRoomId(roomId: String): Long

    fun countByRoomIdAndIdBetween(roomId: String, startId: Long, endId: Long): Long
}
