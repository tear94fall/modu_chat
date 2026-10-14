package com.example.memberservice.application.domain.repository.ro

import com.example.memberservice.application.config.RoRepository
import com.example.memberservice.application.domain.entity.Staff

interface StaffRoRepository : RoRepository<Staff, Long> {

    fun findAll(): List<Staff>

    fun findAllByMemberIdIn(memberIds: Collection<Long>): List<Staff>
}
