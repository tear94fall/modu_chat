package com.example.chatservice.chat.service

import com.example.chatservice.chat.entity.ChatRoom
import com.example.chatservice.chat.entity.ChatRoomMember
import com.example.chatservice.chat.repository.ChatRepository
import com.example.chatservice.chat.repository.ChatRoomMemberRepository
import com.example.chatservice.chat.repository.ChatRoomRepository
import com.example.chatservice.common.exception.CustomException
import com.example.chatservice.kafka.producer.KafkaProducerService
import com.example.chatservice.member.client.MemberFeignClient
import com.example.chatservice.member.dto.MemberDto
import com.example.chatservice.member.service.BlockedIdsCache
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.modelmapper.ModelMapper

/**
 * 트랜잭션 프록시가 없는 순수 단위 테스트다.
 * TransactionSynchronizationManager.isSynchronizationActive() 가 false 이므로
 * ChatRoomService 는 커밋 콜백을 등록하지 못하고 즉시 발행한다 — 그 경로를 검증한다.
 */
class ChatRoomServiceTest {

    private lateinit var chatRoomMemberRepository: ChatRoomMemberRepository
    private lateinit var chatRoomRepository: ChatRoomRepository
    private lateinit var chatRepository: ChatRepository
    private lateinit var memberFeignClient: MemberFeignClient
    private lateinit var kafkaProducerService: KafkaProducerService
    private lateinit var blockedIdsCache: BlockedIdsCache
    private lateinit var chatRoomService: ChatRoomService

    @BeforeEach
    fun setUp() {
        chatRoomMemberRepository = mock()
        chatRoomRepository = mock()
        chatRepository = mock()
        memberFeignClient = mock()
        kafkaProducerService = mock()
        blockedIdsCache = mock()
        whenever(blockedIdsCache.get(anyOrNull())).thenReturn(setOf())

        chatRoomService = ChatRoomService(
            chatRoomMemberRepository,
            chatRoomRepository,
            chatRepository,
            memberFeignClient,
            ModelMapper(),
            kafkaProducerService,
            blockedIdsCache,
        )

        // save 는 넘겨받은 엔티티를 그대로 돌려준다 - JPA 저장 흉내.
        whenever(chatRoomRepository.save(any<ChatRoom>())).thenAnswer { it.getArgument(0) }
    }

    private fun member(id: Long) = MemberDto(id = id, userId = "user-$id")

    @Test
    @DisplayName("방을 만들면 topic-chat-room-created 로 roomId 를 발행한다")
    fun createChatRoom_publishesRoomCreatedEvent() {
        whenever(memberFeignClient.getMembersById(any())).thenReturn(listOf(member(1L), member(2L)))

        val chatRoomDto = chatRoomService.createChatRoom(listOf(1L, 2L))

        verify(kafkaProducerService).sendRoomCreatedMessage(chatRoomDto.roomId!!)
        assertThat(chatRoomDto.roomId).isNotBlank()
    }

    @Test
    @DisplayName("멤버를 하나도 못 찾으면 방을 만들지 않고 발행도 하지 않는다")
    fun createChatRoom_withNoMembers_doesNotPublish() {
        whenever(memberFeignClient.getMembersById(any())).thenReturn(listOf())

        try {
            chatRoomService.createChatRoom(listOf(999L))
        } catch (ignored: CustomException) {
            // 멤버 없음은 예외로 처리된다 - 여기서 검증하려는 것은 발행 여부다.
        }

        verify(kafkaProducerService, never()).sendRoomCreatedMessage(any())
    }

    /** 이미 저장된 방 하나를 흉내 낸다. 넘긴 memberId 마다 ChatRoomMember 를 붙인다. */
    private fun existingRoom(roomId: String, vararg memberIds: Long): ChatRoom {
        val room = ChatRoom(roomId, "새로운 채팅방", "", "", "", "2026-09-08 00:00:00")
        for (memberId in memberIds) {
            room.chatRoomMemberList.add(ChatRoomMember(memberId, "", room))
        }
        return room
    }

    @Test
    @DisplayName("같은 멤버 구성의 방이 있으면 새로 만들지 않고 그 방을 돌려준다")
    fun createChatRoom_withSameMemberSet_returnsExistingRoom() {
        val existing = existingRoom("room-existing", 1L, 2L)
        whenever(memberFeignClient.getMembersById(any())).thenReturn(listOf(member(1L), member(2L)))
        whenever(chatRoomMemberRepository.findRoomIdByExactMemberIds(setOf(1L, 2L))).thenReturn(Optional.of(10L))
        whenever(chatRoomRepository.findById(10L)).thenReturn(Optional.of(existing))

        val chatRoomDto = chatRoomService.createChatRoom(listOf(2L, 1L))

        assertThat(chatRoomDto.roomId).isEqualTo("room-existing")
        assertThat(chatRoomDto.members).extracting("id").containsExactlyInAnyOrder(1L, 2L)
        verify(chatRoomRepository, never()).save(any<ChatRoom>())
        verify(kafkaProducerService, never()).sendRoomCreatedMessage(any())
    }

    @Test
    @DisplayName("같은 멤버 구성의 방이 없으면 새 방을 만들고 발행한다")
    fun createChatRoom_withoutMatchingRoom_createsNewRoom() {
        whenever(memberFeignClient.getMembersById(any())).thenReturn(listOf(member(1L), member(2L)))
        whenever(chatRoomMemberRepository.findRoomIdByExactMemberIds(any())).thenReturn(Optional.empty())

        val chatRoomDto = chatRoomService.createChatRoom(listOf(1L, 2L))

        verify(chatRoomMemberRepository).findRoomIdByExactMemberIds(setOf(1L, 2L))
        verify(chatRoomRepository).save(any<ChatRoom>())
        verify(kafkaProducerService).sendRoomCreatedMessage(chatRoomDto.roomId!!)
    }

    @Test
    @DisplayName("요청 id 에 중복이 있어도 멤버는 한 번만 등록한다")
    fun createChatRoom_withDuplicateIds_registersEachMemberOnce() {
        whenever(memberFeignClient.getMembersById(listOf(1L, 2L))).thenReturn(listOf(member(1L), member(2L)))

        chatRoomService.createChatRoom(listOf(1L, 1L, 2L))

        val saved = argumentCaptor<ChatRoom>()
        verify(chatRoomRepository, times(1)).save(saved.capture())
        assertThat(saved.firstValue.chatRoomMemberList)
            .extracting<Long> { it.memberId }
            .containsExactlyInAnyOrder(1L, 2L)
    }

    @Test
    @DisplayName("초대하면 member-service 가 돌려준 회원을 한 번씩만 추가하고, 이미 있는 회원은 건너뛴다")
    fun addMemberChatRoom_addsInvitedOnce_andSkipsExistingMembers() {
        val room = ChatRoom("room-1", "새로운 채팅방", "", "", "", "2026-09-13 00:00:00")
        room.chatRoomMemberList.add(ChatRoomMember(1L, "", room))
        whenever(chatRoomRepository.findByRoomId("room-1")).thenReturn(Optional.of(room))
        whenever(memberFeignClient.getMembersByUserId(listOf("user-1", "user-2"))).thenReturn(listOf(member(1L), member(2L)))
        // member-service 는 실제 초대된 회원 목록을 돌려준다(이미 있는 1 도 같이 돌려주는 상황).
        whenever(memberFeignClient.inviteChatRoom(any())).thenReturn(listOf(member(1L), member(2L)))

        chatRoomService.addMemberChatRoom("room-1", listOf("user-1", "user-2"))

        assertThat(room.chatRoomMemberList)
            .extracting<Long> { it.memberId }
            .containsExactlyInAnyOrder(1L, 2L)
        // 예전 버그: 초대 전 목록과 응답 목록으로 두 번 저장해 같은 회원이 두 줄씩 생겼다.
        verify(chatRoomMemberRepository, times(1)).saveAll(any<List<ChatRoomMember>>())
    }

    @Test
    @DisplayName("속한 방이 없으면 member-service 를 호출하지 않고 빈 목록을 돌려준다")
    fun searchChatRoomByUserId_withNoRooms_returnsEmptyWithoutMemberLookup() {
        whenever(chatRoomMemberRepository.findAllByMemberId(48L)).thenReturn(listOf())

        val rooms = chatRoomService.searchChatRoomByUserId("48")

        assertThat(rooms).isEmpty()
        verify(memberFeignClient, never()).getMembersById(any())
    }

    @Test
    fun exitAllChatRooms_removesMemberships_andDeletesRoomsLeftEmpty() {
        val shared = ChatRoom("같이 있는 방")
        val meInShared = ChatRoomMember(7L, "0", shared)
        val otherInShared = ChatRoomMember(8L, "0", shared)
        shared.chatRoomMemberList.addAll(listOf(meInShared, otherInShared))
        val alone = ChatRoom("혼자 남은 방")
        val meAlone = ChatRoomMember(7L, "0", alone)
        alone.chatRoomMemberList.add(meAlone)

        whenever(chatRoomMemberRepository.findAllByMemberId(7L)).thenReturn(listOf(meInShared, meAlone))

        val left = chatRoomService.exitAllChatRooms(7L)

        // 두 방 모두에서 내 멤버 행이 지워진다.
        verify(chatRoomMemberRepository).delete(meInShared)
        verify(chatRoomMemberRepository).delete(meAlone)
        assertThat(shared.chatRoomMemberList).containsExactly(otherInShared)
        // 남는 사람이 있는 방은 그대로, 비게 된 방은 지운다.
        verify(chatRoomRepository, never()).delete(shared)
        verify(chatRoomRepository).delete(alone)
        assertThat(left).hasSize(2)
    }

    @Test
    fun exitAllChatRooms_withNoRooms_doesNothing() {
        whenever(chatRoomMemberRepository.findAllByMemberId(9L)).thenReturn(listOf())

        assertThat(chatRoomService.exitAllChatRooms(9L)).isEmpty()
        verify(chatRoomMemberRepository, never()).delete(any<ChatRoomMember>())
        verify(chatRoomRepository, never()).delete(any<ChatRoom>())
    }
}
