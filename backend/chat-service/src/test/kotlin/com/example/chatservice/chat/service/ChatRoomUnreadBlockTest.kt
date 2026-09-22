package com.example.chatservice.chat.service

import com.example.chatservice.chat.entity.ChatRoom
import com.example.chatservice.chat.entity.ChatRoomMember
import com.example.chatservice.chat.repository.ChatRepository
import com.example.chatservice.chat.repository.ChatRoomMemberRepository
import com.example.chatservice.chat.repository.ChatRoomRepository
import com.example.chatservice.kafka.producer.KafkaProducerService
import com.example.chatservice.member.client.MemberFeignClient
import com.example.chatservice.member.dto.MemberDto
import com.example.chatservice.member.service.BlockedIdsCache
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.modelmapper.ModelMapper

/**
 * 미읽음 개수의 차단 제외.
 *
 * 경로 변수 이름은 {userId} 지만 값은 member id(숫자 PK) 다 — 안드로이드가 myMemberId 를
 * 넣는다. 차단 조회는 userId(구글 sub) 기준이라 서비스가 한 번 변환해야 하고,
 * 그 변환이 실패하면 게이트웨이가 넣어 준 X-Auth-User-Id 로 떨어진다. 여기서 그 세 갈래를 본다.
 */
class ChatRoomUnreadBlockTest {

    companion object {
        private const val MEMBER_ID = "7"
        private const val MY_USER_ID = "google-sub-7"
        private val BLOCKED = setOf("bad")
    }

    private lateinit var chatRoomMemberRepository: ChatRoomMemberRepository
    private lateinit var chatRepository: ChatRepository
    private lateinit var memberFeignClient: MemberFeignClient
    private lateinit var blockedIdsCache: BlockedIdsCache
    private lateinit var chatRoomService: ChatRoomService

    @BeforeEach
    fun setUp() {
        chatRoomMemberRepository = mock()
        chatRepository = mock()
        memberFeignClient = mock()
        blockedIdsCache = mock()

        chatRoomService = ChatRoomService(
            chatRoomMemberRepository,
            mock<ChatRoomRepository>(),
            chatRepository,
            memberFeignClient,
            ModelMapper(),
            mock<KafkaProducerService>(),
            blockedIdsCache,
        )

        whenever(memberFeignClient.getMembersById(any())).thenReturn(listOf(MemberDto(id = 7L, userId = MY_USER_ID)))
        whenever(blockedIdsCache.get(anyOrNull())).thenReturn(setOf())
        whenever(blockedIdsCache.get(MY_USER_ID)).thenReturn(BLOCKED)
        whenever(chatRepository.findAllByIdIn(any())).thenReturn(listOf())
        whenever(chatRepository.countByRoomIdAndIdBetween(any(), any(), any(), any())).thenReturn(4L)
    }

    /** 멤버 수만큼 사람이 있고 마지막 메시지가 10번, 내가 읽은 것은 5번인 방. */
    private fun roomWith(memberCount: Int) {
        val chatRoom = ChatRoom("room-1", "room", "", "", "10", "2026-09-13 00:00:00")
        val me = ChatRoomMember(7L, "5", chatRoom)
        chatRoom.chatRoomMemberList.add(me)
        for (i in 1 until memberCount) {
            chatRoom.chatRoomMemberList.add(ChatRoomMember(100L + i, "5", chatRoom))
        }
        whenever(chatRoomMemberRepository.findAllByMemberId(7L)).thenReturn(listOf(me))
    }

    @Test
    @DisplayName("1:1 방은 member id 를 userId 로 바꿔 조회한 차단 목록을 개수 질의에 넘긴다")
    fun oneOnOneUsesResolvedUserId() {
        roomWith(2)

        val result = chatRoomService.searchUnreadChatRoom(MEMBER_ID, null)

        verify(blockedIdsCache).get(MY_USER_ID)
        verify(chatRepository).countByRoomIdAndIdBetween("room-1", 6L, 10L, BLOCKED)
        assertThat(result).hasSize(1)
        assertThat(result[0].unreadChatCount).isEqualTo(4L)
    }

    @Test
    @DisplayName("단체방은 제외 없이 센다")
    fun groupRoomIsNotFiltered() {
        roomWith(3)

        chatRoomService.searchUnreadChatRoom(MEMBER_ID, null)

        verify(chatRepository).countByRoomIdAndIdBetween("room-1", 6L, 10L, setOf())
    }

    @Test
    @DisplayName("member-service 로 userId 변환이 안 되면 X-Auth-User-Id 로 차단 목록을 찾는다")
    fun fallsBackToAuthHeaderWhenResolutionFails() {
        roomWith(2)
        whenever(memberFeignClient.getMembersById(any())).thenThrow(RuntimeException("member-service down"))
        whenever(blockedIdsCache.get("header-user")).thenReturn(setOf("bad2"))

        chatRoomService.searchUnreadChatRoom(MEMBER_ID, "header-user")

        verify(chatRepository).countByRoomIdAndIdBetween("room-1", 6L, 10L, setOf("bad2"))
    }

    @Test
    @DisplayName("신원을 전혀 못 찾으면 예전처럼 제외 없이 센다")
    fun noIdentityMeansNoFilter() {
        roomWith(2)
        whenever(memberFeignClient.getMembersById(any())).thenReturn(listOf())

        chatRoomService.searchUnreadChatRoom(MEMBER_ID, null)

        verify(chatRepository).countByRoomIdAndIdBetween(eq("room-1"), eq(6L), eq(10L), eq(setOf()))
    }
}
