package com.example.profileservice.application.domain.repository.ro

import com.example.profileservice.application.config.RoRepository
import com.example.profileservice.application.domain.entity.Profile

interface ProfileRoRepository : RoRepository<Profile, Long>, ProfileRoCustomRepository {

    fun findByMemberId(memberId: Long): List<Profile>
}
