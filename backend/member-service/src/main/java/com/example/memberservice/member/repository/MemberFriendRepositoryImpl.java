package com.example.memberservice.member.repository;

import static com.example.memberservice.member.entity.QMember.member;
import static com.example.memberservice.member.entity.QMemberFriend.memberFriend;

import com.example.memberservice.member.entity.MemberFriend;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class MemberFriendRepositoryImpl implements MemberFriendCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<MemberFriend> findPage(Long memberId, FriendSort sort, Pageable pageable) {
        JPAQuery<MemberFriend> query = queryFactory
                .selectFrom(memberFriend)
                .join(memberFriend.friend, member).fetchJoin()
                .where(memberFriend.member.id.eq(memberId))
                .orderBy(sort.orders().toArray(new OrderSpecifier<?>[0]));

        if (pageable.isPaged()) {
            query.offset(pageable.getOffset()).limit(pageable.getPageSize());
        }
        List<MemberFriend> content = query.fetch();

        if (pageable.isUnpaged()) {
            return new PageImpl<>(content);
        }
        // 첫 페이지에 다 들어오면 count 질의를 생략한다.
        if (pageable.getOffset() == 0 && content.size() < pageable.getPageSize()) {
            return new PageImpl<>(content, pageable, content.size());
        }
        Long total = queryFactory.select(memberFriend.count()).from(memberFriend)
                .where(memberFriend.member.id.eq(memberId)).fetchOne();
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    public List<MemberFriend> findAllByMemberIdWithFriend(Long memberId) {
        return queryFactory
                .selectFrom(memberFriend)
                .join(memberFriend.friend, member).fetchJoin()
                .where(memberFriend.member.id.eq(memberId))
                .fetch();
    }
}
