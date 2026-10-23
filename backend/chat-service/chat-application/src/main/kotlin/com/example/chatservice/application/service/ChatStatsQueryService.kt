package com.example.chatservice.application.service

import com.example.chatservice.application.config.RoJpaConfig
import com.example.chatservice.application.domain.entity.ChatType
import com.example.chatservice.application.domain.repository.ro.ChatRoRepository
import com.example.chatservice.application.domain.repository.ro.ChatRoomRoRepository
import com.example.chatservice.application.usecase.result.ChatStatsDay
import com.example.chatservice.application.usecase.result.ChatStatsResult
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 백오피스 홈의 채팅 숫자(replica). chat_time 은 UTC `yyyy-MM-dd HH:mm:ss` 문자열이라 같은 꼴의 문자열로 범위를 잘라 받고
 * 여기서 표시 시간대의 날짜로 묶는다(사전식 비교 = 시간순). 방은 created_date(UTC).
 */
@Service
@Transactional(transactionManager = RoJpaConfig.TRANSACTION_MANAGER, readOnly = true)
class ChatStatsQueryService(
    private val chats: ChatRoRepository,
    private val rooms: ChatRoomRoRepository,
    private val clock: Clock,
) {
    fun stats(days: Int, zone: ZoneId): ChatStatsResult {
        val window = DayWindow(zone, days, clock)
        val chatTimes = chats.findChatTimesBetween(window.fromStored.format(CHAT_TIME), window.toStored.format(CHAT_TIME), ChatType.SYSTEM_TYPES)
        val messages = window.byDate(chatTimes.mapNotNull { parseChatTime(it) }) { it }
        val created = window.byDate(rooms.findCreatedDatesBetween(window.fromStored, window.toStored)) { it }
        return ChatStatsResult(zone.id, window.dates.map { ChatStatsDay(it, messages[it]?.size ?: 0, created[it]?.size ?: 0) })
    }

    private fun parseChatTime(text: String): LocalDateTime? = runCatching { LocalDateTime.parse(text, CHAT_TIME) }.getOrNull()

    companion object {
        private val CHAT_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
