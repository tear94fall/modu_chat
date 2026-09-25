package com.example.memberservice.staff

import org.springframework.data.jpa.repository.JpaRepository

interface StaffRepository : JpaRepository<Staff, Long> {
    fun findAllByMemberIdIn(memberIds: Collection<Long>): List<Staff>
}
