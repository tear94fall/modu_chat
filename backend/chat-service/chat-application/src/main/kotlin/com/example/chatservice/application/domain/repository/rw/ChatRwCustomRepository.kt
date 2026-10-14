package com.example.chatservice.application.domain.repository.rw

import com.example.chatservice.application.domain.entity.Chat

/** 쓰기 직후에 읽히는 조회와 쓰기 전 검사. 질의 자체는 replica 쪽과 같다(ChatQueries). */
interface ChatRwCustomRepository {

    fun findByRoomIdAndChatId(roomId: String, chatId: Long): Chat?

    fun findByRoomIdSize(roomId: String, size: Long, blockedSenders: Collection<String>): List<Chat>

    fun findAllByIdIn(ids: List<Long>, blockedSenders: Collection<String>): List<Chat>

    fun countByRoomIdAndIdBetween(roomId: String, startId: Long, endId: Long, blockedSenders: Collection<String>): Long
}
