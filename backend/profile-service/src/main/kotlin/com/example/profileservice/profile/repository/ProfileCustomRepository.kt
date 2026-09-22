package com.example.profileservice.profile.repository

import com.example.profileservice.profile.entity.Profile

interface ProfileCustomRepository {
    fun findByMemberProfile(memberId: Long, id: Long): Profile?

    fun findLatestProfile(memberId: Long): Profile?

    fun findByMemberProfileOffset(memberId: Long, id: Long, count: Long): List<Profile>

    fun findMemberTotalProfiles(memberId: Long): Long

    fun deleteByMemberProfile(memberId: Long, id: Long): Long
}
