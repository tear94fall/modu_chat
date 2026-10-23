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
}
