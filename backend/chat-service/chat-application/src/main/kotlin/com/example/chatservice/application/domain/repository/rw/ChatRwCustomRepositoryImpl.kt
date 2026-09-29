package com.example.chatservice.application.domain.repository.rw

import com.example.chatservice.application.config.QueryDslConfig
import com.example.chatservice.application.domain.entity.Chat
import com.example.chatservice.application.domain.repository.query.ChatQueries
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.beans.factory.annotation.Qualifier

class ChatRwCustomRepositoryImpl(
    @Qualifier(QueryDslConfig.RW_QUERY_FACTORY) queryFactory: JPAQueryFactory,
) : ChatRwCustomRepository {

    private val queries = ChatQueries(queryFactory)

    override fun findByRoomIdAndChatId(roomId: String, chatId: Long): Chat? = queries.findByRoomIdAndChatId(roomId, chatId)

    override fun findByRoomIdSize(roomId: String, size: Long, blockedSenders: Collection<String>): List<Chat> =
        queries.findByRoomIdSize(roomId, size, blockedSenders)

    override fun findAllByIdIn(ids: List<Long>, blockedSenders: Collection<String>): List<Chat> =
        queries.findAllByIdIn(ids, blockedSenders)

    override fun countByRoomIdAndIdBetween(roomId: String, startId: Long, endId: Long, blockedSenders: Collection<String>): Long =
        queries.countByRoomIdAndIdBetween(roomId, startId, endId, blockedSenders)
}
