package com.example.profileservice.profile.repository;

import com.example.profileservice.profile.entity.Profile;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.example.profileservice.profile.entity.QProfile.*;

@RequiredArgsConstructor
public class ProfileRepositoryImpl implements ProfileCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Profile findByMemberProfile(Long memberId, Long id) {
        return queryFactory
                .selectFrom(profile)
                .where(profile.memberId.eq(memberId)
                        .and(profile.id.eq(id)))
                .fetchOne();
    }

    @Override
    public Profile findLatestProfile(Long memberId) {
        return queryFactory
                .selectFrom(profile)
                .where(profile.memberId.eq(memberId))
                .orderBy(profile.createdDate.desc())
                .limit(1)
                .fetchOne();
    }

    @Override
    public List<Profile> findByMemberProfileOffset(Long memberId, Long id, Long count) {
        return queryFactory
                .selectFrom(profile)
                .where(profile.memberId.eq(memberId)
                        .and(profile.id.lt(id)))
                .orderBy(profile.createdDate.desc())
                .limit(count)
                .fetch();
    }

    @Override
    public Long findMemberTotalProfiles(Long memberId) {
        return queryFactory
                .select(Wildcard.count)
                .from(profile)
                .fetch().get(0);
    }

    @Override
    public Long deleteByMemberProfile(Long memberId, Long id) {
        return queryFactory
                .delete(profile)
                .where(profile.memberId.eq(memberId)
                        .and(profile.id.eq(id)))
                .execute();
    }
}
