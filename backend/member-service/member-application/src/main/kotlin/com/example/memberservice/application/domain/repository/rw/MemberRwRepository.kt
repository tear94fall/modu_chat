package com.example.memberservice.application.domain.repository.rw

import com.example.memberservice.application.config.RwRepository
import com.example.memberservice.application.domain.entity.Member
import java.util.Optional

/** 회원 쓰기와, 쓰기 직후·쓰기 전에 최신이어야 하는 읽기(master). */
interface MemberRwRepository : RwRepository<Member, Long> {

    fun existsByEmail(email: String): Boolean

    fun existsByUserId(userId: String): Boolean

    fun findByUserId(userId: String): Optional<Member>

    fun findByEmail(email: String): Optional<Member>

    fun findAllByUserIdIn(userIds: List<String>): List<Member>
}
