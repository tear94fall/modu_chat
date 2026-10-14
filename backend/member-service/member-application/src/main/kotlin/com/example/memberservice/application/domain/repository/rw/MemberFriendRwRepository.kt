package com.example.memberservice.application.domain.repository.rw

import com.example.memberservice.application.config.RwRepository
import com.example.memberservice.application.domain.entity.MemberFriend
import com.example.memberservice.application.domain.repository.query.MemberFriendQueries
import com.example.memberservice.application.domain.repository.query.MemberFriendQueryOperations
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.Optional
import org.springframework.beans.factory.annotation.Qualifier

/** 친구 쓰기와, 쓰기 직후 읽히는 조회(차단 목록·친구 한 명)(master). */
interface MemberFriendRwRepository : RwRepository<MemberFriend, Long>, MemberFriendRwCustomRepository {

    fun findByMemberIdAndFriendId(memberId: Long, friendId: Long): Optional<MemberFriend>

    fun countByMemberId(memberId: Long): Long

    /** 회원 탈퇴: 내가 추가한 친구와 나를 추가한 친구 행을 모두 지운다. */
    fun deleteAllByMember_IdOrFriend_Id(memberId: Long, friendMemberId: Long)
}

interface MemberFriendRwCustomRepository : MemberFriendQueryOperations

class MemberFriendRwCustomRepositoryImpl(
    @Qualifier("rwQueryFactory") queryFactory: JPAQueryFactory,
) : MemberFriendRwCustomRepository, MemberFriendQueryOperations by MemberFriendQueries(queryFactory)
