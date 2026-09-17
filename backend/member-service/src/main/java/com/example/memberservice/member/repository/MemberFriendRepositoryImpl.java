package com.example.memberservice.member.repository;

import static com.example.memberservice.member.entity.QMember.member;
import static com.example.memberservice.member.entity.QMemberFriend.memberFriend;

import com.example.memberservice.member.entity.FriendStatus;
import com.example.memberservice.member.entity.MemberFriend;
import com.example.memberservice.member.entity.QMember;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
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
        return findPage(memberId, null, sort, pageable);
    }

    @Override
    public Page<MemberFriend> findPage(Long memberId, FriendFilter filter, FriendSort sort, Pageable pageable) {
        BooleanExpression status = filter == null ? null : filter.predicate();

        JPAQuery<MemberFriend> query = queryFactory
                .selectFrom(memberFriend)
                .join(memberFriend.friend, member).fetchJoin()
                .where(memberFriend.member.id.eq(memberId), status)
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
                .where(memberFriend.member.id.eq(memberId), status).fetchOne();
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

    @Override
    public List<String> findFriendUserIdsByStatus(Long memberId, FriendStatus status) {
        return queryFactory
                .select(memberFriend.friend.userId)
                .from(memberFriend)
                .join(memberFriend.friend, member)
                .where(memberFriend.member.id.eq(memberId), memberFriend.status.eq(status))
                .fetch();
    }

    /**
     * 역방향 조회. 정방향과 달리 member 쪽을 select 하고 friend 쪽을 조건으로 건다.
     * 같은 별칭(member)으로 두 번 조인할 수 없어 QMember 를 하나 더 만든다.
     */
    @Override
    public List<String> findMemberUserIdsByFriendUserIdAndStatus(String friendUserId, FriendStatus status) {
        QMember blocker = new QMember("blocker");

        return queryFactory
                .select(blocker.userId)
                .from(memberFriend)
                .join(memberFriend.member, blocker)
                .join(memberFriend.friend, member)
                .where(member.userId.eq(friendUserId), memberFriend.status.eq(status))
                .fetch();
    }
}
