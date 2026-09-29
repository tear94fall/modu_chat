package com.example.chatservice.application.domain.repository.ro

import com.example.chatservice.application.config.QueryDslConfig
import com.example.chatservice.application.domain.entity.ChatRoom
import com.example.chatservice.application.domain.entity.QChat.chat
import com.example.chatservice.application.domain.entity.QChatRoom.chatRoom
import com.example.chatservice.application.domain.entity.QChatRoomMember.chatRoomMember
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.beans.factory.annotation.Qualifier

class ChatRoomRoCustomRepositoryImpl(
    @Qualifier(QueryDslConfig.RO_QUERY_FACTORY) private val queryFactory: JPAQueryFactory,
) : ChatRoomRoCustomRepository {

    override fun findAllQueryDsl(): List<ChatRoom> =
        queryFactory
            .selectFrom(chatRoom)
            .leftJoin(chatRoom.chat, chat)
            .fetchJoin()
            .fetch()

    override fun countAll(): Long =
        queryFactory
            .select(chatRoom.count())
            .from(chatRoom)
            .fetchOne() ?: 0L

    override fun findAllByMemberId(memberId: Long): List<ChatRoom> =
        queryFactory
            .select(chatRoom)
            .from(chatRoom)
            .join(chatRoom.chatRoomMemberList, chatRoomMember)
            .where(chatRoomMember.memberId.eq(memberId))
            .fetch()
}
