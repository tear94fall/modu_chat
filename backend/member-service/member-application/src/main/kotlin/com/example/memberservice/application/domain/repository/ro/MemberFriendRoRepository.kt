package com.example.memberservice.application.domain.repository.ro

import com.example.memberservice.application.config.RoRepository
import com.example.memberservice.application.domain.entity.MemberFriend
import com.example.memberservice.application.domain.repository.query.MemberFriendQueries
import com.example.memberservice.application.domain.repository.query.MemberFriendQueryOperations
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.beans.factory.annotation.Qualifier

/** 친구 읽기(replica). 친구 목록·이름표·백오피스 친구 탭이 쓴다. */
interface MemberFriendRoRepository : RoRepository<MemberFriend, Long>, MemberFriendRoCustomRepository {

    fun countByMemberId(memberId: Long): Long
}

interface MemberFriendRoCustomRepository : MemberFriendQueryOperations

class MemberFriendRoCustomRepositoryImpl(
    @Qualifier("roQueryFactory") queryFactory: JPAQueryFactory,
) : MemberFriendRoCustomRepository, MemberFriendQueryOperations by MemberFriendQueries(queryFactory)
