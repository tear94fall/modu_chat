package com.example.chatservice.application.domain.repository.ro

import com.example.chatservice.application.config.QueryDslConfig
import com.example.chatservice.application.domain.entity.Chat
import com.example.chatservice.application.domain.repository.query.ChatQueries
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Pageable

class ChatRoCustomRepositoryImpl(
    @Qualifier(QueryDslConfig.RO_QUERY_FACTORY) queryFactory: JPAQueryFactory,
) : ChatRoCustomRepository {

    private val queries = ChatQueries(queryFactory)

    override fun findByMessage(roomId: String, message: String): List<Chat> = queries.findByMessage(roomId, message)

    override fun findByRoomIdPaging(roomId: String, pageable: Pageable): List<Chat> = queries.findByRoomIdPaging(roomId, pageable)

    override fun findAllByRoomId(roomId: String, blockedSenders: Collection<String>): List<Chat> =
        queries.findAllByRoomId(roomId, blockedSenders)

    override fun findByRoomIdSize(roomId: String, size: Long, blockedSenders: Collection<String>): List<Chat> =
        queries.findByRoomIdSize(roomId, size, blockedSenders)

    override fun findByRoomIdAndChatId(roomId: String, chatId: Long, size: Long, blockedSenders: Collection<String>): List<Chat> =
        queries.findByRoomIdAndChatId(roomId, chatId, size, blockedSenders)

    override fun findByImageChatSize(roomId: String, size: Long, blockedSenders: Collection<String>): List<Chat> =
        queries.findByImageChatSize(roomId, size, blockedSenders)
}
