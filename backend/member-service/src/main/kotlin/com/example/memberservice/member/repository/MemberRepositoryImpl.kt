package com.example.memberservice.member.repository

import com.example.memberservice.member.entity.Member
import com.example.memberservice.member.entity.QMember.member
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.util.StringUtils

class MemberRepositoryImpl(private val queryFactory: JPAQueryFactory) : MemberCustomRepository {

    override fun searchForAdmin(keyword: String?, sort: MemberSort, pageable: Pageable): Page<Member> {
        val predicate = keywordPredicate(keyword)

        val query = queryFactory
            .selectFrom(member)
            .where(predicate)
            .orderBy(*sort.orders().toTypedArray())

        if (pageable.isPaged) {
            query.offset(pageable.offset).limit(pageable.pageSize.toLong())
        }
        val content = query.fetch()

        if (pageable.isUnpaged) {
            return PageImpl(content)
        }
        // 첫 페이지에 다 들어오면 count 질의를 생략한다.
        if (pageable.offset == 0L && content.size < pageable.pageSize) {
            return PageImpl(content, pageable, content.size.toLong())
        }
        val total = queryFactory.select(member.count()).from(member).where(predicate).fetchOne()
        return PageImpl(content, pageable, total ?: 0)
    }

    companion object {
        /** keyword 가 비어 있으면 null 을 돌려준다. QueryDSL 은 null where 조건을 조건 없음으로 본다. */
        private fun keywordPredicate(keyword: String?): BooleanExpression? {
            if (!StringUtils.hasText(keyword)) {
                return null
            }
            return member.email.containsIgnoreCase(keyword).or(member.username.containsIgnoreCase(keyword))
        }
    }
}
