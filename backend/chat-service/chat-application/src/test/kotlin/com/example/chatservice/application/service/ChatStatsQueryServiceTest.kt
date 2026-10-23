package com.example.chatservice.application.service

import com.example.chatservice.application.domain.entity.ChatType
import com.example.chatservice.application.domain.repository.ro.ChatRoRepository
import com.example.chatservice.application.domain.repository.ro.ChatRoomRoRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ChatStatsQueryServiceTest {

    private val chats = mock<ChatRoRepository>()
    private val rooms = mock<ChatRoomRoRepository>()
    private val clock = Clock.fixed(Instant.parse("2026-10-22T15:30:00Z"), ZoneOffset.UTC) // 10-23 00:30 KST
    private val service = ChatStatsQueryService(chats, rooms, clock)

    @Test
    @DisplayName("chat_time 문자열을 같은 꼴의 UTC 범위로 받아 표시 시간대의 날짜로 묶는다")
    fun bucketsChatTimesInDisplayZone() {
        whenever(chats.findChatTimesBetween(any(), any(), any())).thenReturn(
            listOf("2026-10-22 15:05:00", "2026-10-22 14:50:00", "2026-10-22 14:40:00", "깨진 값"),
        )
        whenever(rooms.findCreatedDatesBetween(any(), any())).thenReturn(listOf(LocalDateTime.parse("2026-10-22T15:00:00")))

        val result = service.stats(2, ZoneId.of("Asia/Seoul"))

        assertEquals(listOf(LocalDate.parse("2026-10-22"), LocalDate.parse("2026-10-23")), result.days.map { it.date })
        assertEquals(listOf(2, 1), result.days.map { it.messages })
        assertEquals(listOf(0, 1), result.days.map { it.roomsCreated })
        verify(chats).findChatTimesBetween(eq("2026-10-21 15:00:00"), eq("2026-10-23 15:00:00"), eq(ChatType.SYSTEM_TYPES))
    }
}
