package com.example.chatservice.member.repository;

import com.example.chatservice.member.entity.Member;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static com.example.chatservice.member.entity.QMember.member;

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
    public Optional<List<Member>> findAllFriends(List<Long> friendsIds) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(member)
                        .where(member.id.in(friendsIds))
                        .fetch());
    }

    @Override
    public Optional<List<Member>> findAllByUserIds(List<String> userIds) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(member)
                        .where(member.userId.in(userIds))
                        .fetch());
    }

    @Override
    public Optional<List<Member>> findFriendsByEmail(String email) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(member)
                        .where(member.email.eq(email))
                        .fetch());
    }
}

