package com.example.memberservice.member.repository

import com.example.memberservice.member.entity.Member
import java.util.Optional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long>, MemberCustomRepository {
    fun existsByEmail(email: String): Boolean

    fun findByUserId(userId: String): Optional<Member>

    fun findByEmail(email: String): Optional<Member>

    fun findAllByIdIn(ids: List<Long>): List<Member>

    fun findAllByUserIdIn(userIds: List<String>): List<Member>

    fun findAllByEmail(email: String): List<Member>

    fun findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(email: String, username: String, pageable: Pageable): Page<Member>
}
