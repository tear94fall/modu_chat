package com.example.pointservice.application.service

import com.example.pointservice.application.domain.entity.PointTransactionType
import com.example.pointservice.application.domain.repository.ro.PointTransactionRoRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PointStatsQueryServiceTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    private val transactions = mock<PointTransactionRoRepository>()
    private val clock = Clock.fixed(Instant.parse("2026-10-22T15:30:00Z"), seoul) // 원장은 KST 시계
    private val service = PointStatsQueryService(transactions, clock)

    @Test
    @DisplayName("원장의 KST 시각을 그대로 날짜로 묶고 종류별 절댓값 합을 낸다 — UTC 표시면 하루가 밀린다")
    fun sumsByTypeInDisplayZone() {
        whenever(transactions.findTypeAmountCreatedBetween(any(), any())).thenReturn(
            listOf(
                arrayOf(PointTransactionType.EARN, 100L, LocalDateTime.parse("2026-10-23T00:10:00")),
                arrayOf(PointTransactionType.SPEND, -30L, LocalDateTime.parse("2026-10-23T00:20:00")),
                arrayOf(PointTransactionType.EARN, 50L, LocalDateTime.parse("2026-10-22T23:50:00")),
            ),
        )

        val kst = service.stats(2, seoul)
        assertEquals(listOf(LocalDate.parse("2026-10-22"), LocalDate.parse("2026-10-23")), kst.days.map { it.date })
        assertEquals(listOf(50L, 100L), kst.days.map { it.earned })
        assertEquals(listOf(0L, 30L), kst.days.map { it.spent })
        verify(transactions).findTypeAmountCreatedBetween(eq(LocalDateTime.parse("2026-10-22T00:00:00")), eq(LocalDateTime.parse("2026-10-24T00:00:00")))

        val utc = service.stats(2, ZoneId.of("UTC"))
        assertEquals(listOf(LocalDate.parse("2026-10-21"), LocalDate.parse("2026-10-22")), utc.days.map { it.date })
        assertEquals(listOf(0L, 150L), utc.days.map { it.earned }) // 세 건 모두 UTC 10-22
    }
}
