package com.example.chatservice.application.domain.repository.query

import com.example.chatservice.application.domain.entity.Chat
import com.example.chatservice.application.domain.entity.ChatType
import com.example.chatservice.application.domain.entity.QChat.chat
import com.example.chatservice.application.domain.entity.QChatRoomMember.chatRoomMember
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Pageable

/**
 * 메시지 QueryDSL 질의. master(rw)와 replica(ro) 저장소가 같은 질의를 서로 다른 [JPAQueryFactory] 로 돌린다.
 *
 * 차단 제외(blockedSenders)는 1:1 방에서 "내가 차단한 사람" 의 메시지를 빼는 조건이다.
 * 메모리에서 거르지 않고 질의 조건으로 넣는 이유: limit 를 먼저 태우고 걸러 내면 한 페이지가 텅 비어
 * 앱이 더 못 불러온다. 조건을 먼저 넣어야 size 가 채워진다. 비어 있으면 조건을 붙이지 않는다.
 */
class ChatQueries(private val queryFactory: JPAQueryFactory) {

    fun findByRoomIdAndChatId(roomId: String, chatId: Long): Chat? =
        queryFactory
            .selectFrom(chat)
            .where(chat.roomId.eq(roomId).and(chat.id.eq(chatId)))
            .fetchOne()

    fun findByMessage(roomId: String, message: String): List<Chat> =
        queryFactory
            .selectFrom(chat)
            .where(chat.roomId.eq(roomId).and(chat.message.contains(message)))
            .fetch()

    fun findByRoomIdPaging(roomId: String, pageable: Pageable): List<Chat> =
        queryFactory
            .selectFrom(chat)
            .where(chat.roomId.eq(roomId))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .orderBy(chat.chatTime.desc())
            .fetch()

    fun findAllByRoomId(roomId: String, blockedSenders: Collection<String>): List<Chat> =
        queryFactory
            .selectFrom(chat)
            .where(chat.roomId.eq(roomId), notFrom(blockedSenders))
            .fetch()

    fun findByRoomIdSize(roomId: String, size: Long, blockedSenders: Collection<String>): List<Chat> =
        queryFactory
            .selectFrom(chat)
            .where(chat.roomId.eq(roomId), notFrom(blockedSenders))
            .limit(size)
            .orderBy(chat.chatTime.desc())
            .orderBy(chat.id.asc())
            .fetch()

    fun findByRoomIdAndChatId(roomId: String, chatId: Long, size: Long, blockedSenders: Collection<String>): List<Chat> =
        queryFactory
            .selectFrom(chat)
            .where(chat.roomId.eq(roomId).and(chat.id.lt(chatId)), notFrom(blockedSenders))
            .limit(size)
            .orderBy(chat.chatTime.desc())
            .fetch()

    fun findByImageChatSize(roomId: String, size: Long, blockedSenders: Collection<String>): List<Chat> =
        queryFactory
            .selectFrom(chat)
            .where(chat.roomId.eq(roomId).and(chat.chatType.eq(ChatType.CHAT_TYPE_IMAGE)), notFrom(blockedSenders))
            .limit(size)
            .orderBy(chat.chatTime.desc())
            .fetch()

    /**
     * id 목록 조회. 여러 방이 섞여 올 수 있어서 1:1 방인지를 질의 안에서(방 멤버 수 = 2)
     * 판정하고, 그 방의 차단된 발신자만 뺀다. 단체방 메시지는 그대로 남는다.
     */
    fun findAllByIdIn(ids: List<Long>, blockedSenders: Collection<String>): List<Chat> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        return queryFactory
            .selectFrom(chat)
            .where(chat.id.`in`(ids), notFromInOneOnOneRoom(blockedSenders))
            .fetch()
    }

    /** 미읽음 개수. 1:1 방이면 차단한 발신자의 메시지를 빼고 센다. */
    fun countByRoomIdAndIdBetween(roomId: String, startId: Long, endId: Long, blockedSenders: Collection<String>): Long =
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

    companion object {
        private const val ONE_ON_ONE_MEMBER_COUNT = 2L
    }
}
