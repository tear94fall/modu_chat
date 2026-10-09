package com.example.chatservice.application.service

import com.example.chatservice.application.access.RoomMembership
import com.example.chatservice.application.common.exception.CustomException
import com.example.chatservice.application.common.exception.ErrorCode
import com.example.chatservice.application.common.lock.ApiLock
import com.example.chatservice.application.common.lock.LockParam
import com.example.chatservice.application.config.RwJpaConfig
import com.example.chatservice.application.domain.entity.ChatRoom
import com.example.chatservice.application.domain.entity.ChatRoomMember
import com.example.chatservice.application.domain.repository.rw.ChatReactionRwRepository
import com.example.chatservice.application.domain.repository.rw.ChatRoomMemberRwRepository
import com.example.chatservice.application.domain.repository.rw.ChatRoomRwRepository
import com.example.chatservice.application.domain.repository.rw.ChatRwRepository
import com.example.chatservice.application.usecase.command.UpdateChatRoomCommand
import com.example.chatservice.application.usecase.result.ChatRoomResult
import com.example.chatservice.application.usecase.result.CreatedChatRoom
import com.example.chatservice.application.usecase.result.MemberReadCursor
import com.example.chatservice.application.usecase.result.UnreadCount
import com.example.chatservice.application.usecase.result.UnreadRoom
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

/**
 * 방과 방 멤버 쓰기, 그리고 쓰기 직후에 읽히는 조회(master).
 *
 * 방 조회·방 목록·읽음 커서·안 읽은 개수는 앱과 ws-service 가 방 만들기·초대·나가기·읽음 처리 바로 뒤에 부른다.
 * 복제는 비동기라 replica 로 읽으면 방금 만든 방이 없거나 방금 지운 배지가 되살아난다 — 그래서 여기서 master 로 읽는다.
 */
@Service
@Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER)
class ChatRoomCommandService(
    private val chatRoomRepository: ChatRoomRwRepository,
    private val chatRoomMemberRepository: ChatRoomMemberRwRepository,
    private val chatRepository: ChatRwRepository,
    private val chatReactionRepository: ChatReactionRwRepository,
    @Qualifier(RwJpaConfig.TRANSACTION_MANAGER) transactionManager: PlatformTransactionManager,
    private val clock: Clock,
) {

    private val log = LoggerFactory.getLogger(ChatRoomCommandService::class.java)

    /**
     * 방 만들기의 각 단계를 쓰는 트랜잭션들. [createOrGet] 은 트랜잭션 없이 돌고(아래 설명) 단계마다 이걸 새로 연다.
     * REQUIRES_NEW 라서 새 방 넣기가 유니크 키로 실패해 되돌아가도 다른 단계를 오염시키지 않고,
     * 재확인은 앞 사람이 커밋한 행을 보는 새 트랜잭션에서 돈다.
     */
    private val readTransaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        isReadOnly = true
    }

    private val writeTransaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    // ---------- 읽기(master) ----------

    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun findRoom(roomId: String): ChatRoomResult = ChatRoomResult.of(room(roomId))

    /** 방의 멤버 구성. 없는 방이면 [RoomMembership.NONE]. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun membership(roomId: String): RoomMembership =
        chatRoomRepository.findByRoomId(roomId)
            .map { room -> RoomMembership(true, room.chatRoomMemberList.mapNotNull { it.memberId }) }
            .orElse(RoomMembership.NONE)

    /** 회원이 든 방들. 멤버 행 순서 그대로다. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun findRoomsOfMember(memberId: Long): List<ChatRoomResult> =
        chatRoomMemberRepository.findAllByMemberId(memberId).map { ChatRoomResult.of(it.chatRoom!!) }

    /** 회원이 든 방 가운데 멤버가 정확히 [memberId] 와 [otherMemberId] 둘인 방. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun findOneOnOneRooms(memberId: Long, firstMemberId: Long?, secondMemberId: Long?): List<ChatRoomResult> =
        chatRoomMemberRepository.findAllByMemberId(memberId)
            .map { it.chatRoom!! }
            .filter { chatRoom ->
                val roomMemberIds = chatRoom.chatRoomMemberList.map { it.memberId }
                roomMemberIds.size == RoomMembership.ONE_ON_ONE_MEMBER_COUNT &&
                    roomMemberIds.contains(firstMemberId) && roomMemberIds.contains(secondMemberId)
            }
            .map { ChatRoomResult.of(it) }

    /** 방 멤버별 읽음 커서. 멤버 행 순서 그대로다. 없는 방이면 404. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun findReadCursors(roomId: String): List<MemberReadCursor> =
        room(roomId).chatRoomMemberList.map { MemberReadCursor(it.memberId, parseIdOrZero(it.lastReadChatId)) }

    /**
     * 안 읽은 개수를 세기 전의 방 상태: 방마다 마지막 메시지 id, 내가 읽은 id, 1:1 여부, 마지막 메시지를 보낸 사람.
     * 방마다 마지막 채팅을 따로 조회하면 N+1 이 되므로 한 번에 가져온다.
     */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun findUnreadRooms(memberId: Long): List<UnreadRoom> {
        val memberships = chatRoomMemberRepository.findAllByMemberId(memberId)
        val senderByChatId = findLastChatSenders(memberships)

        return memberships.map { membership ->
            val chatRoom = membership.chatRoom!!
            // 메시지가 하나도 없는 새 방은 두 값이 모두 비어 있다.
            val lastSendChatId = parseIdOrZero(chatRoom.lastChatId)
            UnreadRoom(
                roomId = chatRoom.roomId,
                lastSendChatId = lastSendChatId,
                lastReadChatId = parseIdOrZero(membership.lastReadChatId),
                oneOnOne = chatRoom.chatRoomMemberList.size == RoomMembership.ONE_ON_ONE_MEMBER_COUNT,
                lastSender = senderByChatId[lastSendChatId],
            )
        }
    }

    /**
     * 방별 안 읽은 개수. 마지막 메시지를 내가 보냈다면 그 방은 이미 본 것으로 본다.
     * 차단([blockedSenders])은 1:1 방에서만 적용한다. 단체방은 서버가 그대로 두고 앱이 거른다.
     */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun countUnread(rooms: List<UnreadRoom>, myUserId: String?, blockedSenders: Set<String>): List<UnreadCount> =
        rooms.map { room ->
            val excluded: Set<String> = if (room.oneOnOne) blockedSenders else emptySet()
            val sentByMe = myUserId != null && myUserId == room.lastSender

            var unreadChatCount = 0L
            if (room.lastSendChatId > room.lastReadChatId && !sentByMe) {
                // BETWEEN 은 양끝을 포함한다. 마지막으로 '읽은' 메시지는 빼야 하므로 +1.
                unreadChatCount = chatRepository.countByRoomIdAndIdBetween(
                    room.roomId!!, room.lastReadChatId + 1, room.lastSendChatId, excluded,
                )
            }
            UnreadCount(room.roomId, room.lastSendChatId, room.lastReadChatId, unreadChatCount)
        }

    private fun findLastChatSenders(memberships: List<ChatRoomMember>): Map<Long, String> {
        val lastChatIds = memberships
            .map { parseIdOrZero(it.chatRoom!!.lastChatId) }
            .filter { it > 0 }
            .distinct()
        if (lastChatIds.isEmpty()) {
            return emptyMap()
        }

        val out = HashMap<Long, String>()
        for (chat in chatRepository.findAllByIdIn(lastChatIds)) {
            val sender = chat.sender ?: continue
            out.putIfAbsent(chat.id!!, sender)
        }
        return out
    }

    // ---------- 쓰기 ----------

    /**
     * 멤버 구성이 정확히 같은 방이 있으면 그 방을, 없으면 새 방을 돌려준다(1:1·단체 공통).
     *
     * 왜 막아야 하나: "있나 확인 → 없으면 생성" 사이에 같은 구성의 요청이 끼면 방이 둘 생긴다
     * (서로에게 동시에 대화를 여는 두 사람).
     *
     * 두 겹으로 막는다
     * - 1차 @ApiLock (Redis): 같은 구성의 요청을 줄 세운다. 기다리는 동안 DB 커넥션을 쓰지 않는다.
     *   대기 10초, Redis 가 죽으면 이 겹은 건너뛴다(fail-open).
     * - 2차 `chat_room.member_key` 의 유니크 키: 실제로 중복을 막는 겹. Redis 가 죽어도 DB 가 막는다.
     *   잠금 테이블도, 격리 수준 변경도 없다 — 유니크 키가 대상 테이블에 있다.
     *
     * 실행 순서(이 메서드는 트랜잭션이 아니다. 단계마다 짧은 트랜잭션을 따로 연다)
     * 1. Redis 락을 잡는다.
     * 2. 멤버 목록 조회로 같은 구성의 방을 찾는다(읽기 트랜잭션). 있으면 created = false 로 돌려준다.
     * 3. 없으면 `member_key` 와 함께 새 방을 넣는다(쓰기 트랜잭션).
     * 4. 3이 유니크 키로 막히면 같은 구성을 다른 요청이 먼저 만든 것이다. 새 트랜잭션에서 다시 찾아
     *    그 방을 created = false 로 돌려준다. 되돌아간 쓰기는 자기 트랜잭션만 되돌리므로 2·4에 영향이 없다.
     *
     * 유니크 키는 "같은 구성으로 동시에 만들기" 만 막는다. 방을 찾는 일은 늘 멤버 목록 조회가 한다.
     * 멤버가 바뀐 방은 [ChatRoom.membershipChanged] 로 키가 null 이 되어 다시는 부딪히지 않는다.
     */
    @ApiLock(prefix = RoomMemberSet.LOCK_PREFIX, waitTime = 10L, timeUnit = TimeUnit.SECONDS)
    // 클래스에 붙은 @Transactional 을 끈다. 유니크 키 충돌로 되돌아갈 쓰기를 바깥 트랜잭션과 섞지 않기 위해서다.
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, propagation = Propagation.NOT_SUPPORTED)
    fun createOrGet(@LockParam members: RoomMemberSet): CreatedChatRoom {
        findByExactMembers(members)?.let { return CreatedChatRoom(it, created = false) }

        return try {
            CreatedChatRoom(insertRoom(members), created = true)
        } catch (e: DataIntegrityViolationException) {
            // member_key 유니크 키. 같은 구성의 방을 다른 요청이 먼저 커밋했다. 한 번만 다시 찾는다(되돌지 않는다).
            val winner = findByExactMembers(members)
            if (winner == null) {
                log.warn("room create hit the member_key unique key but no room with the same members exists: {}", members, e)
                throw e
            }
            log.info("room create lost the race for {}, returning the existing room {}", members, winner.roomId)
            CreatedChatRoom(winner, created = false)
        }
    }

    /** 멤버 구성이 정확히 같은 방. 없으면 null. 호출마다 새 읽기 트랜잭션이라 앞 요청이 방금 커밋한 방도 보인다. */
    private fun findByExactMembers(members: RoomMemberSet): ChatRoomResult? = readTransaction.execute {
        chatRoomMemberRepository.findRoomIdByExactMemberIds(members.ids)
            .flatMap { chatRoomRepository.findById(it) }
            .map { ChatRoomResult.of(it) }
            .orElse(null)
    }

    /** 새 방과 멤버 행을 넣는다. 같은 `member_key` 가 이미 있으면 DataIntegrityViolationException. */
    private fun insertRoom(members: RoomMemberSet): ChatRoomResult = writeTransaction.execute {
        // legacy: 새 방의 마지막 대화 시각은 만든 시각이다.
        val createdAt = LocalDateTime.now(clock).format(CHAT_TIME_FORMAT)
        val chatRoom = ChatRoom(UUID.randomUUID().toString(), NEW_ROOM_NAME, "", "", "", createdAt, members.memberKey)
        addNewMembers(chatRoom, members.ids)
        // saveAndFlush: 유니크 키 위반을 커밋이 아니라 이 호출에서 터뜨린다. 그래야 Spring Data 가
        // DataIntegrityViolationException 으로 바꿔 주고(커밋 때 터지면 JpaSystemException 이 될 수 있다) 아래서 잡을 수 있다.
        ChatRoomResult.of(chatRoomRepository.saveAndFlush(chatRoom))
    }!!

    /** 방에 없는 회원만 멤버로 더한다. 없는 방이면 404. */
    fun addMembers(roomId: String, memberIds: Collection<Long>): ChatRoomResult {
        val chatRoom = room(roomId)
        val added = addNewMembers(chatRoom, memberIds)
        if (added.isNotEmpty()) {
            chatRoomMemberRepository.saveAll(added)
            // 멤버 구성이 바뀌었다 — 만들 때의 지문은 더 이상 이 방을 설명하지 않는다.
            chatRoom.membershipChanged()
        }
        return ChatRoomResult.of(chatRoomRepository.save(chatRoom))
    }

    /**
     * 방 나가기: 이 회원의 멤버 행을 지운다. 멤버가 없어진 방은 대화·반응째 지운다(탈퇴 정리와 같은 규칙).
     * 이미 나간 방이면 아무것도 바꾸지 않는다. 없는 방이면 404.
     */
    fun removeMember(roomId: String, memberId: Long): ChatRoomResult {
        val chatRoom = room(roomId)
        val memberships = chatRoom.chatRoomMemberList.filter { it.memberId == memberId }
        memberships.forEach { leave(chatRoom, it) }
        return ChatRoomResult.of(chatRoom)
    }

    /**
     * 회원 탈퇴: 이 회원이 든 모든 방에서 멤버 행을 지운다. 비게 된 방은 방째 지운다(대화도 cascade 로 함께).
     *
     * @return 이 회원이 들어 있던 방들의 PK
     */
    fun exitAll(memberId: Long): List<Long?> {
        val memberships = chatRoomMemberRepository.findAllByMemberId(memberId)
        val roomIds = ArrayList<Long?>()
        for (membership in memberships) {
            val chatRoom = membership.chatRoom!!
            roomIds.add(chatRoom.id)
            leave(chatRoom, membership)
        }
        return roomIds
    }

    private fun leave(chatRoom: ChatRoom, membership: ChatRoomMember) {
        chatRoom.chatRoomMemberList.remove(membership)
        chatRoomMemberRepository.delete(membership)
        if (chatRoom.chatRoomMemberList.isEmpty()) {
            chatRoom.roomId?.let { chatReactionRepository.deleteAllByRoomId(it) }
            chatRoomRepository.delete(chatRoom)
            return
        }
        // 남는 방이다. 멤버 구성이 바뀌었으니 만들 때의 지문을 지운다 — 뒤에 원래 구성으로 방을 만들 수 있어야 한다.
        chatRoom.membershipChanged()
    }

    fun update(roomId: String, command: UpdateChatRoomCommand): ChatRoomResult {
        val chatRoom = room(roomId)
        chatRoom.updateChatRoom(
            command.roomName, command.roomImage, command.lastChatMsg, command.lastChatId, command.lastChatTime,
        )
        return ChatRoomResult.of(chatRoomRepository.save(chatRoom))
    }

    /**
     * 이 회원의 읽음 커서를 방의 마지막 메시지로 옮긴다. 없는 방이면 404.
     *
     * @return 방 멤버가 아니어서 옮기지 못했으면 false
     */
    fun markRead(roomId: String, memberId: Long?): Boolean {
        val chatRoom = room(roomId)
        val membership = chatRoom.chatRoomMemberList.firstOrNull { it.memberId == memberId } ?: return false
        membership.updateLastReadChatId(chatRoom.lastChatId)
        return true
    }

    private fun room(roomId: String): ChatRoom =
        chatRoomRepository.findByRoomId(roomId)
            .orElseThrow { CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, roomId) }

    /**
     * 방에 없는 회원만 추가한다. 같은 회원을 두 번 넣으면 방 목록 조회가 그 방을 두 번 돌려주고
     * 코틀린 앱은 방 id 를 목록 키로 쓰기 때문에 바로 죽는다. DB 에도 (방, 회원) 유니크 제약이 있다.
     */
    private fun addNewMembers(chatRoom: ChatRoom, memberIds: Collection<Long>): List<ChatRoomMember> {
        val existing = chatRoom.chatRoomMemberList.map { it.memberId }.toSet()
        return memberIds
            .distinct()
            .filter { id -> !existing.contains(id) }
            .map { id ->
                val chatRoomMember = ChatRoomMember(id, chatRoom.lastChatId, chatRoom)
                chatRoom.chatRoomMemberList.add(chatRoomMember)
                chatRoomMember
            }
    }

    private fun parseIdOrZero(value: String?): Long {
        if (value.isNullOrBlank()) {
            return 0L
        }
        return value.toLongOrNull() ?: 0L
    }

    companion object {
        const val NEW_ROOM_NAME = "새로운 채팅방"
        private val CHAT_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
