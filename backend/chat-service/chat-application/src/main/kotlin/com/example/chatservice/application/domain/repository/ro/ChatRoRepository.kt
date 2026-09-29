package com.example.chatservice.application.domain.repository.ro

import com.example.chatservice.application.config.RoRepository
import com.example.chatservice.application.domain.entity.Chat
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/** 지난 대화 둘러보기(replica). 방금 쓴 메시지를 바로 읽어야 하는 조회는 ChatRwRepository 를 쓴다. */
interface ChatRoRepository : RoRepository<Chat, Long>, ChatRoCustomRepository {

    fun findByRoomId(roomId: String, pageable: Pageable): Page<Chat>

    fun countByRoomId(roomId: String): Long
}
