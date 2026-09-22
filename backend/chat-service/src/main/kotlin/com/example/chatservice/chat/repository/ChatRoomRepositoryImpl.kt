package com.example.chatservice.chat.repository

import com.example.chatservice.chat.entity.ChatRoom
import com.example.chatservice.chat.entity.QChat.chat
import com.example.chatservice.chat.entity.QChatRoom.chatRoom
import com.example.chatservice.chat.entity.QChatRoomMember.chatRoomMember
import com.querydsl.jpa.impl.JPAQueryFactory

class ChatRoomRepositoryImpl(private val queryFactory: JPAQueryFactory) : ChatRoomCustomRepository {

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
            .leftJoin(chatRoom.chatRoomMemberList, chatRoomMember)
            .where(chatRoomMember.memberId.eq(memberId))
            .fetchJoin()
            .fetch()
}
