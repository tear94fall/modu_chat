package com.example.modumessenger.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

class ChatTimeTest {

    @Test
    fun `서버 포맷을 파싱한다`() {
        val parsed = ChatTime.parse("2026-09-12 10:14:00")
        assertEquals(LocalDateTime.of(2026, 9, 12, 10, 14, 0), parsed)
    }

    @Test
    fun `엉뚱한 값은 널`() {
        assertNull(ChatTime.parse("어제"))
        assertNull(ChatTime.parse(null))
        assertNull(ChatTime.parse(""))
    }

    @Test
    fun `포맷은 파싱의 역이다`() {
        val value = "2026-09-12 10:14:00"
        assertEquals(value, ChatTime.format(ChatTime.parse(value)!!))
    }

    @Test
    fun `짧은 시각은 한국어 오전 오후를 쓴다`() {
        assertEquals("오전 10:14", ChatTime.shortTime("2026-09-12 10:14:00", Locale.KOREA, ZoneOffset.UTC))
        assertEquals("오후 10:14", ChatTime.shortTime("2026-09-12 22:14:00", Locale.KOREA, ZoneOffset.UTC))
    }

    @Test
    fun `짧은 시각은 파싱 실패 시 빈 문자열`() {
        assertEquals("", ChatTime.shortTime("", Locale.KOREA, SEOUL))
    }

    // ---------- 서버 시각은 UTC, 화면은 폰 시간대 ----------

    @Test
    fun `서버의 UTC 시각을 폰 시간대로 바꿔 보여 준다`() {
        // 2026-09-22 23:45 KST 에 보낸 파일은 서버에 UTC 14:45 로 저장돼 있었고 화면에 오후 2:45 로 보였다.
        assertEquals("오후 11:45", ChatTime.shortTime("2026-09-22 14:45:03", Locale.KOREA, SEOUL))
        assertEquals(LocalDateTime.of(2026, 9, 22, 23, 45, 3), ChatTime.local("2026-09-22 14:45:03", SEOUL))
    }

    @Test
    fun `날짜도 폰 시간대 기준이다 - UTC 로는 전날인 새벽 메시지`() {
        // 2026-09-23 01:30 KST = 2026-09-22 16:30 UTC
        assertEquals(LocalDate.of(2026, 9, 23), ChatTime.localDate("2026-09-22 16:30:00", SEOUL))
        assertEquals("2026년 9월 23일 수요일", ChatTime.dayDivider("2026-09-22 16:30:00", SEOUL))
        assertNull(ChatTime.localDate("", SEOUL))
        assertEquals("", ChatTime.dayDivider(null, SEOUL))
    }

    @Test
    fun `보내는 시각은 UTC 로 만든다`() {
        val clock = Clock.fixed(Instant.parse("2026-09-24T11:40:00Z"), SEOUL)
        assertEquals("2026-09-24 11:40:00", ChatTime.now(clock))
    }

    @Test
    fun `채팅방 목록 시각은 오늘 시각 어제 올해 날짜 그 이전 연월일`() {
        val today = LocalDate.of(2026, 9, 24)
        // 모두 KST 로 보면: 9/24 20:40, 9/23 09:00, 9/1 12:00, 2025/12/31 23:00
        assertEquals("오후 8:40", ChatTime.listTime("2026-09-24 11:40:00", today, SEOUL))
        assertEquals("어제", ChatTime.listTime("2026-09-23 00:00:00", today, SEOUL))
        assertEquals("9월 1일", ChatTime.listTime("2026-09-01 03:00:00", today, SEOUL))
        assertEquals("2025. 12. 31.", ChatTime.listTime("2025-12-31 14:00:00", today, SEOUL))
        assertEquals("", ChatTime.listTime("", today, SEOUL))
    }

    private companion object {
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
