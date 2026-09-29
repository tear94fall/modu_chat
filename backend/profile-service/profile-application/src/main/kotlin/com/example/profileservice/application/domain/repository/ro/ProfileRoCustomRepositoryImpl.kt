package com.example.profileservice.application.domain.repository.ro

import com.example.profileservice.application.domain.entity.Profile
import com.example.profileservice.application.domain.entity.QProfile.profile
import com.querydsl.core.types.dsl.Wildcard
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.beans.factory.annotation.Qualifier

class ProfileRoCustomRepositoryImpl(
    @Qualifier("roQueryFactory") private val queryFactory: JPAQueryFactory,
) : ProfileRoCustomRepository {

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
}
