package com.example.memberservice.application.service

import com.example.memberservice.application.domain.entity.MemberStatus
import com.example.memberservice.application.domain.entity.ModuService
import com.example.memberservice.application.domain.repository.ro.MemberRoRepository
import com.example.memberservice.application.domain.repository.ro.MemberServiceUsageRoRepository
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
import org.mockito.kotlin.whenever

class MemberStatsQueryServiceTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    private val members = mock<MemberRoRepository>()
    private val usages = mock<MemberServiceUsageRoRepository>()

    // 2026-10-23 00:30 KST = 2026-10-22 15:30 UTC — UTC 로는 아직 어제다.
    private val clock = Clock.fixed(Instant.parse("2026-10-22T15:30:00Z"), ZoneOffset.UTC)
    private val service = MemberStatsQueryService(members, usages, clock)

    @Test
    @DisplayName("가입은 표시 시간대의 날짜로 묶고, 창의 모든 날짜를 0 포함해 돌려준다")
    fun signupsAreBucketedInDisplayZone() {
        whenever(members.findCreatedDatesBetween(any(), any())).thenReturn(
            listOf(
                LocalDateTime.parse("2026-10-22T15:10:00"), // 10-23 00:10 KST → 오늘
                LocalDateTime.parse("2026-10-22T14:59:00"), // 10-22 23:59 KST → 어제
                LocalDateTime.parse("2026-10-21T00:00:00"), // 10-21 09:00 KST
            ),
        )
        whenever(members.countByStatus(MemberStatus.ACTIVE)).thenReturn(42L)
        whenever(usages.countByServiceSince(any())).thenReturn(listOf(arrayOf(ModuService.CHAT, 5L)))

        val result = service.stats(3, seoul)

        assertEquals(listOf(LocalDate.parse("2026-10-21"), LocalDate.parse("2026-10-22"), LocalDate.parse("2026-10-23")), result.days.map { it.date })
        assertEquals(listOf(1, 1, 1), result.days.map { it.signups })
        assertEquals(42L, result.totalMembers)
        assertEquals(listOf("CHAT" to 5L, "COMMERCE" to 0L), result.active.map { it.service to it.today })
    }

    @Test
    @DisplayName("조회 창은 표시 시간대의 첫날 0시부터 내일 0시까지를 UTC 로 바꾼 범위다")
    fun queryWindowIsConvertedToUtc() {
        whenever(members.findCreatedDatesBetween(any(), any())).thenReturn(emptyList())
        whenever(usages.countByServiceSince(any())).thenReturn(emptyList())

        service.stats(7, seoul)

        // 10-17 00:00 KST = 10-16 15:00 UTC, 10-24 00:00 KST = 10-23 15:00 UTC
        org.mockito.kotlin.verify(members).findCreatedDatesBetween(eq(LocalDateTime.parse("2026-10-16T15:00:00")), eq(LocalDateTime.parse("2026-10-23T15:00:00")))
        // 활동 회원 "오늘" 은 10-23 00:00 KST = 10-22 15:00 UTC 이후
        org.mockito.kotlin.verify(usages).countByServiceSince(eq(LocalDateTime.parse("2026-10-22T15:00:00")))
    }
}
