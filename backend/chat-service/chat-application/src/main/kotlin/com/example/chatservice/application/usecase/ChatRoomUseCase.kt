package com.example.chatservice.application.usecase

import com.example.chatservice.application.access.ChatAccessGuard
import com.example.chatservice.application.common.exception.CustomException
import com.example.chatservice.application.common.exception.ErrorCode
import com.example.chatservice.application.event.ChatEventPublisher
import com.example.chatservice.application.member.BlockedIdsCache
import com.example.chatservice.application.member.MemberGateway
import com.example.chatservice.application.member.MemberInfo
import com.example.chatservice.application.service.ChatRoomCommandService
import com.example.chatservice.application.service.RoomMemberSet
import com.example.chatservice.application.usecase.command.UpdateChatRoomCommand
import com.example.chatservice.application.usecase.result.ChatRoomView
import com.example.chatservice.application.usecase.result.ReadCursor
import com.example.chatservice.application.usecase.result.UnreadCount
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 채팅방 유스케이스. DB 작업(ChatRoomCommandService, 한 호출 = 한 트랜잭션)과 member-service 호출·Kafka 발행을
 * 트랜잭션 밖에서 차례로 잇는다. 앱용은 먼저 [ChatAccessGuard] 로 요청한 사람을 확인한다.
 */
@Component
class ChatRoomUseCase(
    private val chatRoomCommandService: ChatRoomCommandService,
    private val memberGateway: MemberGateway,
    private val chatEventPublisher: ChatEventPublisher,
    private val blockedIdsCache: BlockedIdsCache,
    private val accessGuard: ChatAccessGuard,
) {

    private val log = LoggerFactory.getLogger(ChatRoomUseCase::class.java)

    // ---------- 앱 ----------

    /** 내가 든 방 목록. [id] 는 내 회원 id(숫자) 또는 내 userId 여야 한다. */
    fun roomsOf(authUserId: String?, id: String): List<ChatRoomView> {
        val caller = accessGuard.caller(authUserId)
        accessGuard.requireSelf(caller, id)

        val rooms = chatRoomCommandService.findRoomsOfMember(caller.memberId)
        if (rooms.isEmpty()) {
            // 빈 id 목록으로 member-service 를 부르면 경로 변수가 비어 404 가 난다. 방이 없으면 바로 빈 목록.
            return emptyList()
        }

        val members = memberGateway.byIds(rooms.flatMap { it.memberIds })
        return rooms.map { room -> ChatRoomView(room, members.filter { m -> room.memberIds.any { it == m.id } }) }
    }

    /** 방 정보와 멤버. 방 멤버만 볼 수 있다. */
    fun room(authUserId: String?, roomId: String): ChatRoomView {
        accessGuard.requireRoomMember(accessGuard.caller(authUserId), roomId)
        return roomForInternal(roomId)
    }

    /** 나([userId])와 상대 둘만 있는 방. 없으면 빈 목록. */
    fun oneOnOneRooms(authUserId: String?, userId: String, otherUserId: String): List<ChatRoomView> {
        val caller = accessGuard.caller(authUserId)
        accessGuard.requireSelf(caller, userId)

        val members = memberGateway.byUserIds(arrayListOf(caller.userId, otherUserId))
        if (members.size < 2) {
            return emptyList()
        }
        return chatRoomCommandService.findOneOnOneRooms(caller.memberId, members[0].id, members[1].id)
            .map { ChatRoomView(it, members) }
    }

    /**
     * 방 만들기. 멤버 목록에 나 자신이 들어 있어야 한다. 같은 멤버 구성의 방이 있으면 그 방을 돌려준다.
     * 방 생성 이벤트는 커밋 뒤에 보낸다 — ws-service 가 이벤트를 받고 바로 방을 조회하기 때문이다.
     */
    fun create(authUserId: String?, ids: List<Long>): ChatRoomView {
        val caller = accessGuard.caller(authUserId)
        if (!ids.contains(caller.memberId)) throw CustomException(ErrorCode.FORBIDDEN)

        val members = memberGateway.byIds(ids.distinct())
        if (members.isEmpty()) {
            throw CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, ids.toString())
        }

        val created = chatRoomCommandService.createOrGet(RoomMemberSet(members.mapNotNull { it.id }))
        if (!created.created) {
            return ChatRoomView(created.room, members)
        }

        try {
            chatEventPublisher.roomCreated(created.room.roomId!!)
        } catch (e: Exception) {
            // 방은 이미 만들어졌다. 알림이 빠지면 상대는 다음 방 목록 조회에서 방을 보게 된다.
            log.warn("room {} created but the room-created event was not sent", created.room.roomId, e)
        }
        return ChatRoomView(created.room, emptyList())
    }

    /**
     * 방 나가기. 내 멤버 행을 지우고(비게 된 방은 방째 지운다) member-service 의 내 방 목록에서도 뺀다.
     * member-service 반영이 실패해도 나가기는 끝난 것이다 — 방 목록의 원본은 이 서비스의 멤버 행이다.
     */
    fun leave(authUserId: String?, roomId: String, userId: String): ChatRoomView {
        val caller = accessGuard.caller(authUserId)
        accessGuard.requireSelf(caller, userId)

        val room = chatRoomCommandService.removeMember(roomId, caller.memberId)
        try {
            memberGateway.exit(room.id, listOf(MemberInfo(id = caller.memberId, userId = caller.userId)))
        } catch (e: Exception) {
            log.warn("left room {} but member-service was not updated for member {}", roomId, caller.memberId, e)
        }
        return ChatRoomView(room, emptyList())
    }

    /** 방 정보 수정. 방 멤버만 할 수 있다. */
    fun update(authUserId: String?, roomId: String, command: UpdateChatRoomCommand): ChatRoomView {
        accessGuard.requireRoomMember(accessGuard.caller(authUserId), roomId)
        return updateForInternal(roomId, command)
    }

    /**
     * 초대. 방 멤버만 초대할 수 있다. member-service 에 초대를 알린 뒤 그 응답(실제 초대된 회원) 가운데
     * 아직 방에 없는 사람만 멤버로 더한다.
     */
    fun invite(authUserId: String?, roomId: String, userIds: List<String>): ChatRoomView {
        accessGuard.requireRoomMember(accessGuard.caller(authUserId), roomId)

        val room = chatRoomCommandService.findRoom(roomId)
        val members = memberGateway.byUserIds(userIds)
        if (members.isEmpty()) {
            throw CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userIds.toString())
        }

        val invited = memberGateway.invite(room.id, members)
        return ChatRoomView(chatRoomCommandService.addMembers(roomId, invited.mapNotNull { it.id }), emptyList())
    }

    /**
     * 방별 안 읽은 개수. [id] 는 내 회원 id(안드로이드) 또는 내 userId(iOS)다.
     * 차단 목록은 방 상태를 읽은 뒤, 개수를 세기 전에 트랜잭션 밖에서 받는다.
     */
    fun unread(authUserId: String?, id: String): List<UnreadCount> {
        val caller = accessGuard.caller(authUserId)
        accessGuard.requireSelf(caller, id)

        val rooms = chatRoomCommandService.findUnreadRooms(caller.memberId)
        val blocked = blockedIdsCache.get(caller.userId)
        return chatRoomCommandService.countUnread(rooms, caller.userId, blocked)
    }

    /** 방 읽음 처리. [id] 는 내 회원 id 또는 내 userId 다. 방 멤버가 아니면 400. */
    fun markRead(authUserId: String?, roomId: String, id: String) {
        val caller = accessGuard.caller(authUserId)
        accessGuard.requireSelf(caller, id)

        if (!chatRoomCommandService.markRead(roomId, caller.memberId)) {
            throw CustomException(ErrorCode.USERID_NOT_FOUND, id)
        }
    }

    /** 방 멤버별 읽음 커서. 방 멤버만 볼 수 있다. */
    fun readCursors(authUserId: String?, roomId: String): List<ReadCursor> {
        accessGuard.requireRoomMember(accessGuard.caller(authUserId), roomId)

        val cursors = chatRoomCommandService.findReadCursors(roomId)
        if (cursors.isEmpty()) {
            return emptyList()
        }

        // chat.sender 는 회원 id 가 아니라 userId 라서 member-service 에서 한 번에 바꿔 내려준다.
        // member-service 가 죽으면 빈 목록을 준다. 숫자가 안 뜨는 것이 오류보다 낫다.
        val userIdByMemberId: Map<Long, String> = try {
            val out = HashMap<Long, String>()
            for (member in memberGateway.byIds(cursors.mapNotNull { it.memberId })) {
                val userId = member.userId ?: continue
                out.putIfAbsent(member.id!!, userId)
            }
            out
        } catch (e: Exception) {
            log.warn("failed to resolve userIds for room {}, returning no cursors", roomId, e)
            return emptyList()
        }

        return cursors.mapNotNull { cursor ->
            val userId = userIdByMemberId[cursor.memberId] ?: return@mapNotNull null
            ReadCursor(userId, cursor.lastReadChatId)
        }
    }

    // ---------- 내부(ws-service · chat-store-service · member-service) ----------

    /** ws-service 가 방 생성 이벤트를 받자마자 부른다. */
    fun roomForInternal(roomId: String): ChatRoomView {
        val room = chatRoomCommandService.findRoom(roomId)
        return ChatRoomView(room, memberGateway.byIds(room.memberIds))
    }

    /** userId 가 방 멤버인지. 아니면 400. */
    fun requireMember(roomId: String, userId: String): String {
        if (roomForInternal(roomId).members.none { it.userId == userId }) {
            throw CustomException(ErrorCode.INVALID_CHAT_ROOM_MEMBER, roomId)
        }
        return userId
    }

    fun updateForInternal(roomId: String, command: UpdateChatRoomCommand): ChatRoomView =
        ChatRoomView(chatRoomCommandService.update(roomId, command), emptyList())

    /**
     * ws-service 의 READ 프레임. 식별자 형태가 둘이다: 회원 id(숫자 PK) 또는 userId 문자열.
     * 먼저 회원 id 로 보고, 방 멤버 중에 없으면(혹은 숫자가 아니면) userId 로 보고 member-service 에서 한 번 바꾼다.
     */
    fun markReadForInternal(roomId: String, id: String) {
        // userId(모두 계정 subject)는 Long 범위를 넘는 21자리 숫자라 null 로 떨어진다.
        if (chatRoomCommandService.markRead(roomId, id.toLongOrNull())) {
            return
        }
        val member = memberGateway.byUserId(id)
        if (!chatRoomCommandService.markRead(roomId, member.id)) {
            throw CustomException(ErrorCode.USERID_NOT_FOUND, id)
        }
    }

    /** 회원 탈퇴(member-service). member-service 가 부르므로 member-service 를 다시 부르지 않는다. */
    fun exitAll(memberId: Long): List<Long?> = chatRoomCommandService.exitAll(memberId)
}
