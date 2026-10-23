package com.example.chatservice.application.domain.repository.ro

import com.example.chatservice.application.config.RoRepository
import com.example.chatservice.application.domain.entity.Chat
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/** 지난 대화 둘러보기(replica). 방금 쓴 메시지를 바로 읽어야 하는 조회는 ChatRwRepository 를 쓴다. */
interface ChatRoRepository : RoRepository<Chat, Long>, ChatRoCustomRepository {

    fun findByRoomId(roomId: String, pageable: Pageable): Page<Chat>

    fun countByRoomId(roomId: String): Long

    /** 보낸 시각(UTC `yyyy-MM-dd HH:mm:ss` 문자열)만(백오피스 통계). [from, to) 는 같은 꼴의 문자열, 시스템 안내 행은 뺀다. */
    @Query("select c.chatTime from Chat c where c.chatTime >= :from and c.chatTime < :to and c.chatType not in :systemTypes")
    fun findChatTimesBetween(@Param("from") from: String, @Param("to") to: String, @Param("systemTypes") systemTypes: Collection<Int>): List<String>
}
