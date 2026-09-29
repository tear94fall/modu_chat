package com.example.memberservice.application.domain.repository.ro

import com.example.memberservice.application.config.RoRepository
import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.repository.query.MemberQueries
import com.example.memberservice.application.domain.repository.query.MemberQueryOperations
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.Optional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.beans.factory.annotation.Qualifier

/** 회원 읽기(replica). 목록·검색·남의 프로필 보기처럼 둘러보는 조회가 쓴다. */
interface MemberRoRepository : RoRepository<Member, Long>, MemberRoCustomRepository {

    fun findById(id: Long): Optional<Member>

    fun existsById(id: Long): Boolean

    fun findAllById(ids: Iterable<Long>): List<Member>

    fun findByUserId(userId: String): Optional<Member>

    fun existsByEmail(email: String): Boolean

    fun findAllByEmail(email: String): List<Member>

    fun findAllByUserIdIn(userIds: List<String>): List<Member>

    fun findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(email: String, username: String, pageable: Pageable): Page<Member>
}

interface MemberRoCustomRepository : MemberQueryOperations

class MemberRoCustomRepositoryImpl(
    @Qualifier("roQueryFactory") queryFactory: JPAQueryFactory,
) : MemberRoCustomRepository, MemberQueryOperations by MemberQueries(queryFactory)
