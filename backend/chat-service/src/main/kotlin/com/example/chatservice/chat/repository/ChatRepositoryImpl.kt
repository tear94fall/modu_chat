package com.example.chatservice.chat.repository

import com.example.chatservice.chat.entity.Chat
import com.example.chatservice.chat.entity.ChatType
import com.example.chatservice.chat.entity.QChat.chat
import com.example.chatservice.chat.entity.QChatRoomMember.chatRoomMember
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Pageable

class ChatRepositoryImpl(private val queryFactory: JPAQueryFactory) : ChatCustomRepository {

    companion object {
        private const val ONE_ON_ONE_MEMBER_COUNT = 2L
    }

    override fun findByRoomIdAndChatId(roomId: String, chatId: Long): Chat? =
        queryFactory
            .selectFrom(chat)
            .where(chat.roomId.eq(roomId).and(chat.id.eq(chatId)))
            .fetchOne()

    override fun findByMessage(roomId: String, message: String): List<Chat> =
        queryFactory
            .selectFrom(chat)
            .where(chat.roomId.eq(roomId).and(chat.message.contains(message)))
            .fetch()

    override fun findByRoomIdPaging(roomId: String, pageable: Pageable): List<Chat> =
        queryFactory
            .selectFrom(chat)
            .where(chat.roomId.eq(roomId))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .orderBy(chat.chatTime.desc())
            .fetch()

    override fun findByRoomIdSize(roomId: String, size: Long): List<Chat> = findByRoomIdSize(roomId, size, emptyList())

    override fun findByRoomIdAndChatId(roomId: String, chatId: Long, size: Long): List<Chat> =
        findByRoomIdAndChatId(roomId, chatId, size, emptyList())

    override fun findByImageChatSize(roomId: String, size: Long): List<Chat> = findByImageChatSize(roomId, size, emptyList())

    override fun findAllByRoomId(roomId: String, blockedSenders: Collection<String>): List<Chat> =
        queryFactory
            .selectFrom(chat)
            .where(chat.roomId.eq(roomId), notFrom(blockedSenders))
            .fetch()

    override fun findByRoomIdSize(roomId: String, size: Long, blockedSenders: Collection<String>): List<Chat> =
        queryFactory
            .selectFrom(chat)
            .where(chat.roomId.eq(roomId), notFrom(blockedSenders))
            .limit(size)
            .orderBy(chat.chatTime.desc())
            .orderBy(chat.id.asc())
            .fetch()

    override fun findByRoomIdAndChatId(roomId: String, chatId: Long, size: Long, blockedSenders: Collection<String>): List<Chat> =
        queryFactory
            .selectFrom(chat)
            .where(chat.roomId.eq(roomId).and(chat.id.lt(chatId)), notFrom(blockedSenders))
            .limit(size)
            .orderBy(chat.chatTime.desc())
            .fetch()

    override fun findByImageChatSize(roomId: String, size: Long, blockedSenders: Collection<String>): List<Chat> =
        queryFactory
            .selectFrom(chat)
            .where(chat.roomId.eq(roomId).and(chat.chatType.eq(ChatType.CHAT_TYPE_IMAGE)), notFrom(blockedSenders))
            .limit(size)
            .orderBy(chat.chatTime.desc())
            .fetch()

    override fun findAllByIdIn(ids: List<Long>, blockedSenders: Collection<String>): List<Chat> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        return queryFactory
            .selectFrom(chat)
            .where(chat.id.`in`(ids), notFromInOneOnOneRoom(blockedSenders))
            .fetch()
    }

    override fun countByRoomIdAndIdBetween(roomId: String, startId: Long, endId: Long, blockedSenders: Collection<String>): Long =
        queryFactory
            .select(chat.count())
            .from(chat)
            .where(chat.roomId.eq(roomId).and(chat.id.between(startId, endId)), notFrom(blockedSenders))
            .fetchOne() ?: 0L

    /**
     * 차단한 발신자 제외 조건. 비어 있으면 null 을 줘서 where 에서 사라진다(= 기존 질의).
     * sender 가 null 인 행이 NOT IN 의 3값 논리에 걸려 통째로 빠지지 않게 isNull 을 함께 본다.
     */
    private fun notFrom(blockedSenders: Collection<String>?): BooleanExpression? {
        if (blockedSenders.isNullOrEmpty()) {
            return null
        }
        return chat.sender.isNull.or(chat.sender.notIn(blockedSenders))
    }

    /**
     * 방이 섞여 들어오는 질의용. "1:1 방이면서 차단한 사람이 보낸" 메시지만 뺀다.
     * 방 멤버 수는 상관 서브쿼리로 센다(멤버가 정확히 2명 = 1:1).
     */
    private fun notFromInOneOnOneRoom(blockedSenders: Collection<String>?): BooleanExpression? {
        if (blockedSenders.isNullOrEmpty()) {
            return null
        }

        val oneOnOneRoom = JPAExpressions
            .select(chatRoomMember.count())
            .from(chatRoomMember)
            .where(chatRoomMember.chatRoom.id.eq(chat.chatRoom.id))
            .eq(ONE_ON_ONE_MEMBER_COUNT)

        return chat.sender.isNull
            .or(chat.sender.notIn(blockedSenders))
            .or(oneOnOneRoom.not())
    }
}
