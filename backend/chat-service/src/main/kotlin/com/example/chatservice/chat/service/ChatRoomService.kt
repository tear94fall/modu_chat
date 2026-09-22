package com.example.chatservice.chat.service

import com.example.chatservice.api.admin.dto.AdminChatRoomSummaryDto
import com.example.chatservice.chat.dto.ChatReadCursorDto
import com.example.chatservice.chat.dto.ChatRoomDto
import com.example.chatservice.chat.dto.ChatRoomLastReadChatDto
import com.example.chatservice.chat.entity.ChatRoom
import com.example.chatservice.chat.entity.ChatRoomMember
import com.example.chatservice.chat.repository.ChatRepository
import com.example.chatservice.chat.repository.ChatRoomMemberRepository
import com.example.chatservice.chat.repository.ChatRoomRepository
import com.example.chatservice.chat.repository.ChatRoomSort
import com.example.chatservice.common.exception.CustomException
import com.example.chatservice.common.exception.ErrorCode
import com.example.chatservice.kafka.producer.KafkaProducerService
import com.example.chatservice.member.client.MemberFeignClient
import com.example.chatservice.member.dto.ChatRoomMemberDto
import com.example.chatservice.member.dto.MemberDto
import com.example.chatservice.member.service.BlockedIdsCache
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import org.modelmapper.ModelMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
@Transactional
class ChatRoomService(
    private val chatRoomMemberRepository: ChatRoomMemberRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRepository: ChatRepository,
    private val memberFeignClient: MemberFeignClient,
    private val modelMapper: ModelMapper,
    private val kafkaProducerService: KafkaProducerService,
    private val blockedIdsCache: BlockedIdsCache,
) {

    private val log = LoggerFactory.getLogger(ChatRoomService::class.java)

    companion object {
        /** 1:1 방의 정의: 방 멤버가 정확히 2명. */
        private const val ONE_ON_ONE_MEMBER_COUNT = 2
    }

    fun searchChatRoomByUserId(memberId: String): List<ChatRoomDto> {
        val chatRoomMemberList = chatRoomMemberRepository.findAllByMemberId(memberId.toLong())
        if (chatRoomMemberList.isEmpty()) {
            // 빈 id 목록으로 member-service 를 부르면 경로 변수가 비어 404 → 500 이 된다. 방이 없으면 바로 빈 목록.
            return emptyList()
        }

        val chatRooms = chatRoomMemberList.map { it.chatRoom!! }

        val memberIds = chatRooms.flatMap { chatRoom -> chatRoom.chatRoomMemberList.map { it.memberId!! } }

        val memberDtoList = memberFeignClient.getMembersById(memberIds)

        return chatRooms.map { chatRoom ->
            val members = memberDtoList.filter { m -> chatRoom.chatRoomMemberList.any { c -> c.memberId == m.id } }
            ChatRoomDto(chatRoom, members)
        }
    }

    fun searchChatRoomByRoomId(roomId: String): ChatRoomDto {
        val chatRoom = chatRoomRepository.findByRoomId(roomId)
            .orElseThrow { CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, roomId) }

        val chatRoomMemberIds = chatRoom.chatRoomMemberList.map { it.memberId!! }

        val members = memberFeignClient.getMembersById(chatRoomMemberIds)

        return ChatRoomDto(chatRoom, members)
    }

    fun searchOneOnOneChatRoom(userId: String, roomUserId: String): List<ChatRoomDto> {
        val member = memberFeignClient.getMember(userId)
        val members = memberFeignClient.getMembersByUserId(arrayListOf(userId, roomUserId))

        val chatRoomMemberList = chatRoomMemberRepository.findAllByMemberId(member.id!!)
        val chatRoomList = chatRoomMemberList
            .map { it.chatRoom!! }
            .filter { chatRoom ->
                val roomMemberId = chatRoom.chatRoomMemberList.map { it.memberId }
                roomMemberId.size == 2 && roomMemberId.contains(members[0].id) && roomMemberId.contains(members[1].id)
            }

        return chatRoomList.map { chatRoom -> ChatRoomDto(chatRoom, members) }
    }

    /**
     * 같은 멤버 구성의 방이 이미 있으면 새로 만들지 않고 그 방을 돌려준다.
     * 클라이언트는 응답의 roomId 로 이동하므로 자연스럽게 기존 방으로 들어간다.
     */
    fun createChatRoom(ids: List<Long>): ChatRoomDto {
        val memberIds = ids.distinct()
        val members = memberFeignClient.getMembersById(memberIds)
        if (members.isEmpty()) {
            throw CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, ids.toString())
        }

        val existingRoom = findChatRoomByExactMembers(members)
        if (existingRoom != null) {
            return ChatRoomDto(existingRoom, members)
        }

        // legacy
        val chatRoomCreateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val chatRoom = ChatRoom(UUID.randomUUID().toString(), "새로운 채팅방", "", "", "", chatRoomCreateTime)

        val chatRoomDto = addNewChatRoomMember(chatRoom, members)

        // ws-service 컨슈머가 이 이벤트를 받고 바로 방을 조회하므로, 커밋 전에 보내면
        // 아직 안 보이는 방을 찾게 된다. 커밋 이후로 미룬다.
        publishRoomCreatedAfterCommit(chatRoomDto.roomId!!)

        return chatRoomDto
    }

    /**
     * 트랜잭션이 커밋된 뒤에만 발행한다. 트랜잭션 동기화가 없는 컨텍스트(단위 테스트 등)에서
     * 호출되면 등록할 트랜잭션이 없으므로 즉시 보낸다.
     */
    private fun publishRoomCreatedAfterCommit(roomId: String) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    kafkaProducerService.sendRoomCreatedMessage(roomId)
                }
            })
        } else {
            kafkaProducerService.sendRoomCreatedMessage(roomId)
        }
    }

    fun exitChatRoomMember(roomId: String, userId: String): ChatRoomDto {
        val chatRoom = chatRoomRepository.findByRoomId(roomId)
            .orElseThrow { CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, roomId) }
        val members = memberFeignClient.getMembersByUserId(arrayListOf(userId))
        if (members.isEmpty()) {
            throw CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId)
        }

        val chatRoomDto = addNewChatRoomMember(chatRoom, members)

        val memberInviteDto = ChatRoomMemberDto(chatRoomDto, members)

        val invited = memberFeignClient.exitChatRoom(memberInviteDto)

        return addNewChatRoomMember(chatRoom, invited)
    }

    /**
     * 회원 탈퇴: 이 회원이 든 모든 방에서 멤버 행을 지운다. 비게 된 방은 방째 지운다(대화도 cascade 로 함께).
     * member-service 가 내부 API 로 부르므로 member-service 를 다시 부르지 않는다(그쪽이 자기 목록을 정리한다).
     *
     * @return 이 회원이 들어 있던 방들의 PK
     */
    @Transactional
    fun exitAllChatRooms(memberId: Long): List<Long?> {
        val memberships = chatRoomMemberRepository.findAllByMemberId(memberId)
        val roomIds = ArrayList<Long?>()
        for (membership in memberships) {
            val room = membership.chatRoom!!
            roomIds.add(room.id)
            room.chatRoomMemberList.remove(membership)
            chatRoomMemberRepository.delete(membership)
            if (room.chatRoomMemberList.isEmpty()) {
                chatRoomRepository.delete(room)
            }
        }
        return roomIds
    }

    fun updateChatRoom(roomId: String, chatRoomDto: ChatRoomDto): ChatRoomDto {
        val findChatRoom = chatRoomRepository.findByRoomId(roomId)
            .orElseThrow { CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, roomId) }

        findChatRoom.updateChatRoom(chatRoomDto)

        val updateChatRoom = chatRoomRepository.save(findChatRoom)
        return modelMapper.map(updateChatRoom, ChatRoomDto::class.java)
    }

    fun addMemberChatRoom(roomId: String, userIds: List<String>): ChatRoomDto {
        val chatRoom = chatRoomRepository.findByRoomId(roomId)
            .orElseThrow { CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, roomId) }
        val members = memberFeignClient.getMembersByUserId(userIds)
        if (members.isEmpty()) {
            throw CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userIds.toString())
        }

        // 예전에는 여기서 members 로 한 번, member-service 응답으로 또 한 번 추가해 같은 회원이 두 줄씩 생겼다.
        // member-service 에 초대를 알린 뒤 그 응답(실제 초대된 회원)만 한 번 추가한다.
        val chatRoomDto = modelMapper.map(chatRoom, ChatRoomDto::class.java)

        val memberInviteDto = ChatRoomMemberDto(chatRoomDto, members)

        val invited = memberFeignClient.inviteChatRoom(memberInviteDto)

        return addNewChatRoomMember(chatRoom, invited)
    }

    /**
     * 방별 안 읽은 개수.
     *
     * memberId 는 경로 변수 이름이 {userId} 지만 실제로는 member id(숫자 PK)다 —
     * 안드로이드가 myMemberId 를 그대로 넣는다(ChatRepository.refreshUnreadCounts).
     * 차단 목록은 userId(구글 sub) 로만 조회할 수 있어서 이미 있는 findMyUserId 로
     * 한 번 변환하고, member-service 가 죽어 변환에 실패하면 게이트웨이가 넣어 준
     * X-Auth-User-Id(authUserId) 로 대신한다. 둘 다 없으면 필터 없이 예전처럼 센다.
     */
    fun searchUnreadChatRoom(memberId: String, authUserId: String?): List<ChatRoomLastReadChatDto> {
        val chatRoomMemberList = chatRoomMemberRepository.findAllByMemberId(memberId.toLong())

        // 마지막 메시지를 내가 보냈다면 그 방은 이미 본 것으로 본다.
        // chat.sender 는 member id 가 아니라 userId 문자열이라 한 번 조회해 둔다.
        val me = findMyUserId(memberId) ?: authUserId
        val senderByChatId = findLastChatSenders(chatRoomMemberList)
        val blockedSenders = blockedIdsCache.get(me)

        return chatRoomMemberList.map { chatRoomMember ->
            val chatRoom = chatRoomMember.chatRoom!!
            val roomId = chatRoom.roomId

            // 메시지가 하나도 없는 새 방은 두 값이 모두 null 이다.
            val lastReadChatId = parseIdOrZero(chatRoomMember.lastReadChatId)
            val lastSendChatId = parseIdOrZero(chatRoom.lastChatId)

            // 차단은 1:1 방에서만 적용한다. 단체방은 서버가 그대로 두고 앱이 거른다.
            val excluded: Set<String> = if (isOneOnOne(chatRoom)) blockedSenders else emptySet()

            var unreadChatCount = 0L
            if (lastSendChatId > lastReadChatId && !sentByMe(senderByChatId, lastSendChatId, me)) {
                // BETWEEN 은 양끝을 포함한다. 마지막으로 '읽은' 메시지는 빼야 하므로 +1.
                unreadChatCount = chatRepository.countByRoomIdAndIdBetween(
                    roomId!!, lastReadChatId + 1, lastSendChatId, excluded,
                )
            }

            ChatRoomLastReadChatDto.createChatRoomLastReadChatDto(roomId, lastSendChatId, lastReadChatId, unreadChatCount)
        }
    }

    private fun isOneOnOne(chatRoom: ChatRoom): Boolean = chatRoom.chatRoomMemberList.size == ONE_ON_ONE_MEMBER_COUNT

    /** 마지막 메시지의 발신자가 나인지. 판단할 수 없으면 false 라서 기존 계산이 그대로 남는다. */
    private fun sentByMe(senderByChatId: Map<Long, String>, lastSendChatId: Long, myUserId: String?): Boolean {
        if (myUserId == null) {
            return false
        }
        return myUserId == senderByChatId[lastSendChatId]
    }

    /** member-service 가 죽어도 배지 조회 자체는 살아야 하므로 실패를 삼키고 null 을 준다. */
    private fun findMyUserId(memberId: String): String? {
        return try {
            val members = memberFeignClient.getMembersById(listOf(memberId.toLong()))
            if (members.isNullOrEmpty()) {
                null
            } else {
                members[0].userId
            }
        } catch (e: Exception) {
            log.warn("failed to resolve userId for member {}, falling back to id-only count", memberId, e)
            null
        }
    }

    /** 방마다 마지막 채팅을 따로 조회하면 N+1 이 된다. 한 번에 가져온다. */
    private fun findLastChatSenders(chatRoomMemberList: List<ChatRoomMember>): Map<Long, String> {
        val lastChatIds = chatRoomMemberList
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

    private fun parseIdOrZero(value: String?): Long {
        if (value.isNullOrBlank()) {
            return 0L
        }
        return value.toLongOrNull() ?: 0L
    }

    /**
     * 방 멤버별 읽음 커서. chat.sender 는 member id 가 아니라 userId 문자열이라
     * member-service 에서 한 번에 변환해 내려준다.
     * member-service 가 죽으면 빈 목록을 준다. 숫자가 안 뜨는 것이 500 보다 낫다.
     */
    fun searchReadCursors(roomId: String): List<ChatReadCursorDto> {
        val chatRoom = chatRoomRepository.findByRoomId(roomId)
            .orElseThrow { CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, roomId) }

        val chatRoomMemberList = chatRoom.chatRoomMemberList
        if (chatRoomMemberList.isEmpty()) {
            return emptyList()
        }

        val memberIds = chatRoomMemberList.map { it.memberId!! }

        val userIdByMemberId: Map<Long, String> = try {
            val out = HashMap<Long, String>()
            for (member in memberFeignClient.getMembersById(memberIds)) {
                val userId = member.userId ?: continue
                out.putIfAbsent(member.id!!, userId)
            }
            out
        } catch (e: Exception) {
            log.warn("failed to resolve userIds for room {}, returning no cursors", roomId, e)
            return emptyList()
        }

        return chatRoomMemberList.mapNotNull { chatRoomMember ->
            val userId = userIdByMemberId[chatRoomMember.memberId] ?: return@mapNotNull null
            ChatReadCursorDto(userId = userId, lastReadChatId = parseIdOrZero(chatRoomMember.lastReadChatId))
        }
    }

    /**
     * 이 엔드포인트는 호출자가 둘이라 식별자 형태가 둘이다.
     * 안드로이드는 REST 로 memberId(숫자, PK) 를 보내고 — 방 목록 안읽음 배지를 지우는
     * 기존 경로 — ws-service 의 READ 프레임은 OAuth userId 문자열을 보낸다.
     * 그래서 먼저 memberId 로 시도하고, 방 멤버 중에 없으면(혹은 애초에 Long 파싱이
     * 안 되면) OAuth userId 로 보고 member-service 에서 한 번 변환한다.
     */
    fun updateLastReadChat(roomId: String, userId: String) {
        val chatRoom = chatRoomRepository.findByRoomId(roomId)
            .orElseThrow { CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, roomId) }

        // OAuth userId 는 Long 범위를 넘는 21자리 숫자라 null 로 떨어진다.
        val directMemberId: Long? = userId.toLongOrNull()

        var findChatRoomMember = chatRoom.chatRoomMemberList.firstOrNull { it.memberId == directMemberId }

        if (findChatRoomMember == null) {
            val member = memberFeignClient.getMember(userId)
            findChatRoomMember = chatRoom.chatRoomMemberList.firstOrNull { it.memberId == member.id }
                ?: throw CustomException(ErrorCode.USERID_NOT_FOUND, userId)
        }

        findChatRoomMember.updateLastReadChatId(chatRoom.lastChatId)
    }

    /** 멤버 id 집합이 정확히 일치하는 방을 DB 집계로 찾는다. 1:1 방과 그룹방 모두 해당한다. */
    private fun findChatRoomByExactMembers(members: List<MemberDto>): ChatRoom? {
        val wantedMemberIds = members.mapNotNull { it.id }.toSet()

        return chatRoomMemberRepository.findRoomIdByExactMemberIds(wantedMemberIds)
            .flatMap { chatRoomRepository.findById(it) }
            .orElse(null)
    }

    /**
     * 방에 없는 회원만 추가한다. 같은 회원을 두 번 넣으면 방 목록 조회가 그 방을 두 번 돌려주고
     * 코틀린 앱은 방 id 를 목록 키로 쓰기 때문에 바로 죽는다. DB 에도 (방, 회원) 유니크 제약이 있다.
     */
    private fun addNewChatRoomMember(chatRoom: ChatRoom, members: List<MemberDto>): ChatRoomDto {
        val existing = chatRoom.chatRoomMemberList.map { it.memberId }.toSet()
        val chatRoomMemberList = members
            .mapNotNull { it.id }
            .distinct()
            .filter { id -> !existing.contains(id) }
            .map { id ->
                val chatRoomMember = ChatRoomMember(id, chatRoom.lastChatId, chatRoom)
                chatRoom.chatRoomMemberList.add(chatRoomMember)
                chatRoomMember
            }

        if (chatRoomMemberList.isNotEmpty()) {
            chatRoomMemberRepository.saveAll(chatRoomMemberList)
        }
        val newRoom = chatRoomRepository.save(chatRoom)

        return modelMapper.map(newRoom, ChatRoomDto::class.java)
    }

    /**
     * 백오피스 방 목록. 멤버 수를 세기 위해 같은 트랜잭션 안에서 컬렉션을 초기화한다.
     * 정렬은 [ChatRoomSort] 가 정한다 — 멤버 수만 엔티티 속성이 아니라서 전용 질의로 간다.
     * pageable 에는 쪽 번호와 크기만 있으면 된다. 정렬은 여기서 붙인다.
     */
    @Transactional(readOnly = true)
    fun searchChatRoomsForAdmin(sort: ChatRoomSort, pageable: Pageable): Page<AdminChatRoomSummaryDto> {
        val request = PageRequest.of(pageable.pageNumber, pageable.pageSize, sort.sort())
        val rooms: Page<ChatRoom> = when (sort) {
            ChatRoomSort.MEMBER_COUNT_ASC -> chatRoomRepository.findAllOrderByMemberCountAsc(request)
            ChatRoomSort.MEMBER_COUNT_DESC -> chatRoomRepository.findAllOrderByMemberCountDesc(request)
            else -> chatRoomRepository.findAll(request)
        }
        return rooms.map { AdminChatRoomSummaryDto(it) }
    }
}
