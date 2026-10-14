package com.example.memberservice.application.domain.repository.rw

import com.example.memberservice.application.config.RwRepository
import com.example.memberservice.application.domain.entity.Staff

interface StaffRwRepository : RwRepository<Staff, Long> {

    fun findAllByMemberIdIn(memberIds: Collection<Long>): List<Staff>
}
