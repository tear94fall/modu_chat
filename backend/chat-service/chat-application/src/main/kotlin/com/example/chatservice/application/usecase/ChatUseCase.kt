package com.example.chatservice.application.usecase

import com.example.chatservice.application.access.ChatAccessGuard
import com.example.chatservice.application.access.RoomMembership
import com.example.chatservice.application.member.BlockedIdsCache
import com.example.chatservice.application.service.ChatCommandService
import com.example.chatservice.application.service.ChatQueryService
import com.example.chatservice.application.service.ChatRoomCommandService
import com.example.chatservice.application.usecase.command.SaveChatCommand
import com.example.chatservice.application.usecase.result.ChatResult
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * 메시지 유스케이스. 앱용(authUserId 를 받는 것)은 먼저 [ChatAccessGuard] 로 요청한 사람과 방 멤버 여부를 확인한다.
 * 차단 목록(member-service)은 DB 트랜잭션 밖에서 받아 질의 조건으로 넘긴다.
 */
@Component
class ChatUseCase(
    private val chatCommandService: ChatCommandService,
    private val chatQueryService: ChatQueryService,
    private val chatRoomCommandService: ChatRoomCommandService,
    private val blockedIdsCache: BlockedIdsCache,
    private val accessGuard: ChatAccessGuard,
) {

    // ---------- 앱 ----------

    /** 메시지 하나. 그 메시지가 있는 방의 멤버만 볼 수 있다. */
    fun chat(authUserId: String?, chatId: Long): ChatResult {
        val caller = accessGuard.caller(authUserId)
        val chat = chatCommandService.findById(chatId)
        chat.roomId?.let { accessGuard.requireRoomMember(caller, it) }
        return chat
    }

    /**
     * id 목록 조회. 여러 방이 섞여도 된다. 내가 멤버가 아닌 방의 메시지는 없는 id 처럼 조용히 뺀다.
     * 1:1 방에서 내가 차단한 사람이 보낸 메시지도 뺀다(단체방은 그대로).
     */
    fun chats(authUserId: String?, chatIds: List<Long>): List<ChatResult> {
        val caller = accessGuard.caller(authUserId)
        val blocked = blockedIdsCache.get(caller.userId)
        val chats = chatCommandService.findAllByIds(chatIds, blocked)
        if (chats.isEmpty()) return chats
        val myRoomIds = chatRoomCommandService.findRoomsOfMember(caller.memberId).mapNotNull { it.roomId }.toSet()
        return chats.filter { it.roomId in myRoomIds }
    }

    fun history(authUserId: String?, roomId: String): List<ChatResult> =
        chatQueryService.findAllByRoom(roomId, blockedSendersIn(authUserId, roomId))

    /** 차단 필터 없는 페이지 조회. */
    fun page(authUserId: String?, roomId: String, pageable: Pageable): List<ChatResult> {
        requireRoomMember(authUserId, roomId)
        return chatQueryService.findPage(roomId, pageable)
    }

    /** 방에 들어갈 때 읽는 최근 메시지. 방금 받은 메시지가 빠지면 안 되므로 master 로 읽는다. */
    fun recent(authUserId: String?, roomId: String, size: Long): List<ChatResult> =
        chatCommandService.findRecent(roomId, size, blockedSendersIn(authUserId, roomId))

    fun before(authUserId: String?, roomId: String, chatId: Long, size: Long): List<ChatResult> =
        chatQueryService.findBefore(roomId, chatId, size, blockedSendersIn(authUserId, roomId))

    fun images(authUserId: String?, roomId: String, size: Long): List<ChatResult> =
        chatQueryService.findImages(roomId, size, blockedSendersIn(authUserId, roomId))

    fun count(authUserId: String?, roomId: String): Long {
        requireRoomMember(authUserId, roomId)
        return chatQueryService.count(roomId)
    }

    /** 방 id 와 메시지 id 가 둘 다 맞는 메시지. 없으면 null. */
    fun chatInRoom(authUserId: String?, roomId: String, chatId: Long): ChatResult? {
        requireRoomMember(authUserId, roomId)
        return chatCommandService.findByRoomIdAndChatId(roomId, chatId)
    }

    /** 차단 필터 없는 본문 검색. */
    fun search(authUserId: String?, roomId: String, message: String): List<ChatResult> {
        requireRoomMember(authUserId, roomId)
        return chatQueryService.search(roomId, message)
    }

    /** 보낸 사람만 지울 수 있다. */
    fun delete(authUserId: String?, roomId: String, chatId: Long): ChatResult {
        val caller = accessGuard.caller(authUserId)
        return chatCommandService.delete(roomId, chatId, caller.userId)
    }

    // ---------- 내부(ws-service · chat-store-service) ----------

    fun save(command: SaveChatCommand): Long? = chatCommandService.save(command)

    /** ws-service 가 방금 저장한 메시지를 바로 읽는다. */
    fun chatForInternal(chatId: Long): ChatResult = chatCommandService.findById(chatId)

    private fun requireRoomMember(authUserId: String?, roomId: String): RoomMembership =
        accessGuard.requireRoomMember(accessGuard.caller(authUserId), roomId)

    /**
     * 이 요청에서 뺄 발신자들. 방 멤버인지 확인하면서 알게 된 멤버 수로 1:1 인지 본다.
     * 단체방(과 없는 방)이면 빈 집합이고, 그때는 member-service 를 부르지도 않는다.
     */
    private fun blockedSendersIn(authUserId: String?, roomId: String): Set<String> {
        val caller = accessGuard.caller(authUserId)
        val membership = accessGuard.requireRoomMember(caller, roomId)
        if (!membership.oneOnOne) {
            return emptySet()
        }
        return blockedIdsCache.get(caller.userId)
    }
}
