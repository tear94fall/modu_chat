package com.example.memberservice.application.domain.repository.ro

import com.example.memberservice.application.config.RoRepository
import com.example.memberservice.application.domain.entity.MemberServiceUsage

interface MemberServiceUsageRoRepository : RoRepository<MemberServiceUsage, Long> {

    fun findAllByUserId(userId: String): List<MemberServiceUsage>

    fun findAllByUserIdIn(userIds: Collection<String>): List<MemberServiceUsage>
}
