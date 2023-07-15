package com.example.profileservice.profile.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import static com.example.profileservice.profile.entity.QProfile.*;

@RequiredArgsConstructor
public class ProfileRepositoryImpl implements ProfileCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Long deleteByMemberProfile(Long memberId, String value) {
        return queryFactory
                .delete(profile)
                .where(profile.memberId.eq(memberId)
                        .and(profile.value.eq(value)))
                .execute();
    }
}
