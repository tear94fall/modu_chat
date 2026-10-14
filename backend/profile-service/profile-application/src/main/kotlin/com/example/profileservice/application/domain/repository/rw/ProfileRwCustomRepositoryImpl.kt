package com.example.profileservice.application.domain.repository.rw

import com.example.profileservice.application.domain.entity.Profile
import com.example.profileservice.application.domain.entity.QProfile.profile
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.beans.factory.annotation.Qualifier

class ProfileRwCustomRepositoryImpl(
    @Qualifier("rwQueryFactory") private val queryFactory: JPAQueryFactory,
) : ProfileRwCustomRepository {

    override fun findByMemberProfile(memberId: Long, id: Long): Profile? =
        queryFactory.selectFrom(profile)
            .where(profile.memberId.eq(memberId).and(profile.id.eq(id)))
            .fetchOne()

    override fun deleteByMemberProfile(memberId: Long, id: Long): Long =
        queryFactory.delete(profile)
            .where(profile.memberId.eq(memberId).and(profile.id.eq(id)))
            .execute()
}
