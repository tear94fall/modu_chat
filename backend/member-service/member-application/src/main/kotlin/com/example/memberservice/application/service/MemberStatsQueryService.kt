package com.example.memberservice.application.service

import com.example.memberservice.application.config.RoJpaConfig
import com.example.memberservice.application.domain.entity.MemberStatus
import com.example.memberservice.application.domain.entity.ModuService
import com.example.memberservice.application.domain.repository.ro.MemberRoRepository
import com.example.memberservice.application.domain.repository.ro.MemberServiceUsageRoRepository
import com.example.memberservice.application.domain.repository.ro.StaffRoRepository
import com.example.memberservice.application.usecase.result.ActiveMembersResult
import com.example.memberservice.application.usecase.result.MemberStatsDay
import com.example.memberservice.application.usecase.result.MemberStatsResult
import com.example.memberservice.application.usecase.result.MemberSummaryResult
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
    private val staff: StaffRoRepository,
    private val clock: Clock,
) {
    /** 회원 목록 위 요약. 서비스 숫자는 활성 회원 기준이라 chat + commerce − any = both, active − any = none 이 성립한다(서비스가 둘일 때만). */
    fun summary(): MemberSummaryResult {
        check(ModuService.entries.size == 2) { "both 계산은 서비스가 둘(CHAT·COMMERCE)일 때만 맞다 — 서비스가 늘면 count 질의로 바꿔라." }
        val active = members.countByStatus(MemberStatus.ACTIVE)
        val byService = usages.countActiveMembersByService().associate { it[0] as ModuService to (it[1] as Number).toLong() }
        val chat = byService[ModuService.CHAT] ?: 0L
        val commerce = byService[ModuService.COMMERCE] ?: 0L
        val any = usages.countActiveMembersWithAnyService()
        return MemberSummaryResult(
            active = active,
            withdrawn = members.countByStatus(MemberStatus.WITHDRAWN),
            staff = staff.count(),
            chat = chat,
            commerce = commerce,
            both = chat + commerce - any,
            none = (active - any).coerceAtLeast(0),
        )
    }

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
