package com.example.memberservice.application.domain.repository.ro

import com.example.memberservice.application.config.RoRepository
import com.example.memberservice.application.domain.entity.MemberServiceUsage
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MemberServiceUsageRoRepository : RoRepository<MemberServiceUsage, Long> {

    fun findAllByUserId(userId: String): List<MemberServiceUsage>

    fun findAllByUserIdIn(userIds: Collection<String>): List<MemberServiceUsage>

    /** 서비스별로 [since] 이후에 이용한 회원 수(백오피스 통계). 각 행은 [service, count]. */
    @Query("select u.service, count(u) from MemberServiceUsage u where u.lastUsedAt >= :since group by u.service")
    fun countByServiceSince(@Param("since") since: LocalDateTime): List<Array<Any>>

    /** 서비스별 이용 회원 수 — 탈퇴하지 않은 회원만(이용 기록은 탈퇴해도 남으므로 회원 상태로 거른다). */
    @Query(
        "select u.service, count(distinct u.userId) from MemberServiceUsage u " +
            "where u.userId in (select m.userId from Member m where m.status = com.example.memberservice.application.domain.entity.MemberStatus.ACTIVE) " +
            "group by u.service",
    )
    fun countActiveMembersByService(): List<Array<Any>>

    /** 서비스를 하나라도 쓴 탈퇴하지 않은 회원 수. */
    @Query(
        "select count(distinct u.userId) from MemberServiceUsage u " +
            "where u.userId in (select m.userId from Member m where m.status = com.example.memberservice.application.domain.entity.MemberStatus.ACTIVE)",
    )
    fun countActiveMembersWithAnyService(): Long
}
