package com.example.modumessenger.member.repository;

import com.example.modumessenger.member.entity.Member;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
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

    @Override
    public List<Member> findAllFriends(List<Long> friendsIds) {
        return queryFactory
                .selectFrom(member)
                .where(member.id.in(friendsIds))
                .fetch();
    }

    @Override
    public List<Member> findFriendsByEmail(String email) {
        return queryFactory
                .selectFrom(member)
                .where(member.email.eq(email))
                .fetch();
    }
}
