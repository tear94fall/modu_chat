package com.example.memberservice.application.service

import com.example.memberservice.application.config.RoJpaConfig
import com.example.memberservice.application.domain.entity.MemberStatus
import com.example.memberservice.application.domain.entity.ModuService
import com.example.memberservice.application.domain.repository.ro.MemberRoRepository
import com.example.memberservice.application.domain.repository.ro.MemberServiceUsageRoRepository
import com.example.memberservice.application.usecase.result.ActiveMembersResult
import com.example.memberservice.application.usecase.result.MemberStatsDay
import com.example.memberservice.application.usecase.result.MemberStatsResult
import java.time.Clock
import java.time.ZoneId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 백오피스 홈의 회원 숫자(replica). 가입은 created_date 를 표시 시간대의 날짜로 묶고, 활동 회원은 member_service_usage 의
 * 마지막 이용 시각(1시간 단위로만 갱신)으로 센다 — 그래서 "오늘·7일" 두 창만 내고 날짜별 추이는 내지 않는다.
 */
@Service
@Transactional(transactionManager = RoJpaConfig.TRANSACTION_MANAGER, readOnly = true)
class MemberStatsQueryService(
    private val members: MemberRoRepository,
    private val usages: MemberServiceUsageRoRepository,
    private val clock: Clock,
) {
    fun stats(days: Int, zone: ZoneId): MemberStatsResult {
        val window = DayWindow(zone, days, clock)
        val signups = window.byDate(members.findCreatedDatesBetween(window.fromStored, window.toStored)) { it }
        val today = window.dates.last()
        val todayCounts = countsSince(window.toStored(today))
        val weekCounts = countsSince(window.toStored(today.minusDays(6)))
        return MemberStatsResult(
            zone = zone.id,
            days = window.dates.map { MemberStatsDay(it, signups[it]?.size ?: 0) },
            totalMembers = members.countByStatus(MemberStatus.ACTIVE),
            active = ModuService.entries.map { ActiveMembersResult(it.name, todayCounts[it] ?: 0L, weekCounts[it] ?: 0L) },
        )
    }

    private fun countsSince(since: java.time.LocalDateTime): Map<ModuService, Long> =
        usages.countByServiceSince(since).associate { it[0] as ModuService to (it[1] as Number).toLong() }
}
