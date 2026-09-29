package com.example.chatservice.application.domain.repository.rw

import com.example.chatservice.application.config.RwRepository
import com.example.chatservice.application.domain.entity.Chat
import java.util.Optional

interface ChatRwRepository : RwRepository<Chat, Long>, ChatRwCustomRepository {

    fun findAllByIdIn(ids: List<Long>): List<Chat>

    /** 메시지를 지운 뒤 방의 마지막 메시지를 다시 정할 때 쓴다. */
    fun findFirstByRoomIdAndIdNotOrderByIdDesc(roomId: String, id: Long): Optional<Chat>
}
