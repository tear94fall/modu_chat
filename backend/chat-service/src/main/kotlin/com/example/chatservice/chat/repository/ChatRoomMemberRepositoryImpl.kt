package com.example.chatservice.chat.repository

import com.example.chatservice.chat.entity.ChatRoomMember
import com.example.chatservice.chat.entity.QChatRoomMember
import com.example.chatservice.chat.entity.QChatRoomMember.chatRoomMember
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.Optional

class ChatRoomMemberRepositoryImpl(private val queryFactory: JPAQueryFactory) : ChatRoomMemberCustomRepository {

    override fun findAllByMemberId(memberId: Long): List<ChatRoomMember> =
        queryFactory
            .selectFrom(chatRoomMember)
            .where(chatRoomMember.memberId.eq(memberId))
            .fetch()

    /**
     * DB 에서 집계로 걸러 방 id 하나만 받는다. 한 멤버가 속한 방으로 범위를 좁힌 뒤,
     * 멤버 수와 요청 집합에 포함된 멤버 수가 모두 요청 집합 크기와 같은 방만 남긴다.
     * 중복 등록된 멤버 행이 있어도 countDistinct 라서 영향을 받지 않는다.
     */
    override fun findRoomIdByExactMemberIds(memberIds: Set<Long>): Optional<Long> {
        if (memberIds.isEmpty()) {
            return Optional.empty()
        }

        val anyMemberId = memberIds.iterator().next()
        val expectedSize = memberIds.size.toLong()
        val roomOfMember = QChatRoomMember("roomOfMember")

        val memberIdIfWanted = CaseBuilder()
            .`when`(chatRoomMember.memberId.`in`(memberIds)).then(chatRoomMember.memberId)
            .otherwise(Expressions.nullExpression(Long::class.java))

        val roomId = queryFactory
            .select(chatRoomMember.chatRoom.id)
            .from(chatRoomMember)
            .where(
                chatRoomMember.chatRoom.id.`in`(
                    JPAExpressions.select(roomOfMember.chatRoom.id)
                        .from(roomOfMember)
                        .where(roomOfMember.memberId.eq(anyMemberId)),
                ),
            )
            .groupBy(chatRoomMember.chatRoom.id)
            .having(
                chatRoomMember.memberId.countDistinct().eq(expectedSize),
                memberIdIfWanted.countDistinct().eq(expectedSize),
            )
            .fetchFirst()

        return Optional.ofNullable(roomId)
    }
}
