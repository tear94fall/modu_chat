package com.example.profileservice.application.domain.repository.rw

import com.example.profileservice.application.config.RwRepository
import com.example.profileservice.application.domain.entity.Profile

interface ProfileRwRepository : RwRepository<Profile, Long>, ProfileRwCustomRepository {

    fun findByMemberId(memberId: Long): List<Profile>
}
