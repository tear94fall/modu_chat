package com.example.memberservice.member.repository;

import static com.example.memberservice.member.entity.QMember.member;

import com.example.memberservice.member.entity.Member;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Member> findFriends(Collection<Long> ids, FriendSort sort, Pageable pageable) {
        if (ids.isEmpty()) {
            return Page.empty(pageable);
        }

        JPAQuery<Member> query = queryFactory
                .selectFrom(member)
                .where(member.id.in(ids))
                .orderBy(sort.orders().toArray(new OrderSpecifier<?>[0]));

        if (pageable.isPaged()) {
            query.offset(pageable.getOffset()).limit(pageable.getPageSize());
        }
        List<Member> content = query.fetch();

        if (pageable.isUnpaged()) {
            return new PageImpl<>(content);
        }
        // 첫 페이지에 다 들어오면 count 질의를 생략한다.
        if (pageable.getOffset() == 0 && content.size() < pageable.getPageSize()) {
            return new PageImpl<>(content, pageable, content.size());
        }

        Long total = queryFactory.select(member.count()).from(member).where(member.id.in(ids)).fetchOne();
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
