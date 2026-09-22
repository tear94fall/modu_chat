package com.example.chatservice.chat.repository

import com.example.chatservice.chat.entity.Chat
import org.springframework.data.domain.Pageable

interface ChatCustomRepository {

    fun findByRoomIdAndChatId(roomId: String, chatId: Long): Chat?

    fun findByMessage(roomId: String, message: String): List<Chat>

    fun findByRoomIdPaging(roomId: String, pageable: Pageable): List<Chat>

    fun findByRoomIdSize(roomId: String, size: Long): List<Chat>

    fun findByRoomIdAndChatId(roomId: String, chatId: Long, size: Long): List<Chat>

    fun findByImageChatSize(roomId: String, size: Long): List<Chat>

    /*
     * 아래 변형들은 1:1 방에서 "내가 차단한 사람" 의 메시지를 뺀다.
     * 메모리에서 거르지 않고 질의 조건으로 넣는 이유: limit 를 먼저 태우고 걸러 내면
     * 한 페이지가 텅 비어 앱이 더 못 불러온다. 조건을 먼저 넣어야 size 가 채워진다.
     * blockedSenders 가 비어 있으면 조건을 붙이지 않으므로 기존 질의와 같다.
     */

    fun findAllByRoomId(roomId: String, blockedSenders: Collection<String>): List<Chat>

    fun findByRoomIdSize(roomId: String, size: Long, blockedSenders: Collection<String>): List<Chat>

    fun findByRoomIdAndChatId(roomId: String, chatId: Long, size: Long, blockedSenders: Collection<String>): List<Chat>

    fun findByImageChatSize(roomId: String, size: Long, blockedSenders: Collection<String>): List<Chat>

    /**
     * id 목록 조회. 여러 방이 섞여 올 수 있어서 1:1 방인지를 질의 안에서(방 멤버 수 = 2)
     * 판정하고, 그 방의 차단된 발신자만 뺀다. 단체방 메시지는 그대로 남는다.
     */
    fun findAllByIdIn(ids: List<Long>, blockedSenders: Collection<String>): List<Chat>

    /** 미읽음 개수. 1:1 방이면 차단한 발신자의 메시지를 빼고 센다. */
    fun countByRoomIdAndIdBetween(roomId: String, startId: Long, endId: Long, blockedSenders: Collection<String>): Long
}
