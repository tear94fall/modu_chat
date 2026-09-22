package com.example.memberservice.member.repository

import com.example.memberservice.member.entity.FriendStatus
import com.example.memberservice.member.entity.MemberFriend
import com.example.memberservice.member.entity.QMember
import com.example.memberservice.member.entity.QMember.member
import com.example.memberservice.member.entity.QMemberFriend.memberFriend
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class MemberFriendRepositoryImpl(private val queryFactory: JPAQueryFactory) : MemberFriendCustomRepository {

    override fun findPage(memberId: Long, sort: FriendSort, pageable: Pageable): Page<MemberFriend> =
        findPage(memberId, null, sort, pageable)

    override fun findPage(memberId: Long, filter: FriendFilter?, sort: FriendSort, pageable: Pageable): Page<MemberFriend> {
        val status = filter?.predicate()

        val query = queryFactory
            .selectFrom(memberFriend)
            .join(memberFriend.friend, member).fetchJoin()
            .where(memberFriend.member.id.eq(memberId), status)
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
        val total = queryFactory.select(memberFriend.count()).from(memberFriend)
            .where(memberFriend.member.id.eq(memberId), status).fetchOne()
        return PageImpl(content, pageable, total ?: 0)
    }

    override fun findAllByMemberIdWithFriend(memberId: Long): List<MemberFriend> =
        queryFactory
            .selectFrom(memberFriend)
            .join(memberFriend.friend, member).fetchJoin()
            .where(memberFriend.member.id.eq(memberId))
            .fetch()

    override fun findFriendUserIdsByStatus(memberId: Long, status: FriendStatus): List<String> =
        queryFactory
            .select(memberFriend.friend.userId)
            .from(memberFriend)
            .join(memberFriend.friend, member)
            .where(memberFriend.member.id.eq(memberId), memberFriend.status.eq(status))
            .fetch()

    /**
     * 역방향 조회. 정방향과 달리 member 쪽을 select 하고 friend 쪽을 조건으로 건다.
     * 같은 별칭(member)으로 두 번 조인할 수 없어 QMember 를 하나 더 만든다.
     */
    override fun findMemberUserIdsByFriendUserIdAndStatus(friendUserId: String, status: FriendStatus): List<String> {
        val blocker = QMember("blocker")

        return queryFactory
            .select(blocker.userId)
            .from(memberFriend)
            .join(memberFriend.member, blocker)
            .join(memberFriend.friend, member)
            .where(member.userId.eq(friendUserId), memberFriend.status.eq(status))
            .fetch()
    }
}
