package com.example.profileservice.profile.repository

import com.example.profileservice.profile.entity.Profile
import com.example.profileservice.profile.entity.QProfile.profile
import com.querydsl.core.types.dsl.Wildcard
import com.querydsl.jpa.impl.JPAQueryFactory

class ProfileRepositoryImpl(private val queryFactory: JPAQueryFactory) : ProfileCustomRepository {

    override fun findByMemberProfile(memberId: Long, id: Long): Profile? =
        queryFactory.selectFrom(profile)
            .where(profile.memberId.eq(memberId).and(profile.id.eq(id)))
            .fetchOne()

    override fun findLatestProfile(memberId: Long): Profile? =
        queryFactory.selectFrom(profile)
            .where(profile.memberId.eq(memberId))
            .orderBy(profile.createdDate.desc())
            .limit(1)
            .fetchOne()

    override fun findByMemberProfileOffset(memberId: Long, id: Long, count: Long): List<Profile> =
        queryFactory.selectFrom(profile)
            .where(profile.memberId.eq(memberId).and(profile.id.lt(id)))
            .orderBy(profile.createdDate.desc())
            .limit(count)
            .fetch()

    /** 옛 자바는 memberId 조건 없이 전체 행을 셌다. 이름대로 그 회원의 것만 센다. */
    override fun findMemberTotalProfiles(memberId: Long): Long =
        queryFactory.select(Wildcard.count)
            .from(profile)
            .where(profile.memberId.eq(memberId))
            .fetchOne() ?: 0L

    override fun deleteByMemberProfile(memberId: Long, id: Long): Long =
        queryFactory.delete(profile)
            .where(profile.memberId.eq(memberId).and(profile.id.eq(id)))
            .execute()
}
