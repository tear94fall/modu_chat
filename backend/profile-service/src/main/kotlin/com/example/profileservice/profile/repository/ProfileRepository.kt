package com.example.profileservice.profile.repository

import com.example.profileservice.profile.entity.Profile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProfileRepository : JpaRepository<Profile, Long>, ProfileCustomRepository {

    fun findByMemberId(memberId: Long): List<Profile>
}
