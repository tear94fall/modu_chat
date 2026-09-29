package com.example.profileservice.application.domain.repository.ro

import com.example.profileservice.application.domain.entity.Profile

interface ProfileRoCustomRepository {

    fun findByMemberProfile(memberId: Long, id: Long): Profile?

    fun findLatestProfile(memberId: Long): Profile?

    fun findByMemberProfileOffset(memberId: Long, id: Long, count: Long): List<Profile>

    fun findMemberTotalProfiles(memberId: Long): Long
}
