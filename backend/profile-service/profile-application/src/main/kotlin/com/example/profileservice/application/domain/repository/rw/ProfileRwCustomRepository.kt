package com.example.profileservice.application.domain.repository.rw

import com.example.profileservice.application.domain.entity.Profile

interface ProfileRwCustomRepository {

    fun findByMemberProfile(memberId: Long, id: Long): Profile?

    fun deleteByMemberProfile(memberId: Long, id: Long): Long
}
