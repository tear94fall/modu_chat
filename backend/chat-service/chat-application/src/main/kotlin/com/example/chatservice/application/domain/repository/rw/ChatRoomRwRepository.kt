package com.example.chatservice.application.domain.repository.rw

import com.example.chatservice.application.config.RwRepository
import com.example.chatservice.application.domain.entity.ChatRoom
import java.util.Optional

interface ChatRoomRwRepository : RwRepository<ChatRoom, Long> {

    fun findByRoomId(roomId: String): Optional<ChatRoom>
}
