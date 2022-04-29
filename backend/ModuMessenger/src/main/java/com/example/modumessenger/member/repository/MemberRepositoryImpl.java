package com.example.modumessenger.member.repository;

import com.example.modumessenger.member.entity.Member;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static com.example.modumessenger.member.entity.QMember.member;

@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Member> searchMemberByUserId(String userId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(member)
                        .where(member.userId.eq(userId))
                        .fetchOne());
    }
}
