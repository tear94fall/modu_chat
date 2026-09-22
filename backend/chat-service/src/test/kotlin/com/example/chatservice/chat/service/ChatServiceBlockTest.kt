package com.example.chatservice.chat.service

import com.example.chatservice.chat.entity.ChatRoom
import com.example.chatservice.chat.entity.ChatRoomMember
import com.example.chatservice.chat.repository.ChatRepository
import com.example.chatservice.chat.repository.ChatRoomRepository
import com.example.chatservice.member.service.BlockedIdsCache
import java.util.Optional
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * 이력 조회가 어떤 제외 집합으로 저장소를 부르는지 본다.
 * 단체방·헤더 없음이면 member-service 를 아예 부르지 않아야 한다(불필요한 왕복).
 */
class ChatServiceBlockTest {

    companion object {
        private const val ROOM = "room-1"
        private val BLOCKED = setOf("bad")
    }

    private lateinit var chatRoomRepository: ChatRoomRepository
    private lateinit var chatRepository: ChatRepository
    private lateinit var blockedIdsCache: BlockedIdsCache
    private lateinit var chatReactionService: ChatReactionService
    private lateinit var chatService: ChatService

    @BeforeEach
    fun setUp() {
        chatRoomRepository = mock()
        chatRepository = mock()
        blockedIdsCache = mock()
        chatReactionService = mock()
        whenever(chatReactionService.withReactions(any())).thenAnswer { it.getArgument(0) }
        chatService = ChatService(chatRoomRepository, chatRepository, org.modelmapper.ModelMapper(), blockedIdsCache, chatReactionService)

        whenever(blockedIdsCache.get(any())).thenReturn(BLOCKED)
        whenever(chatRepository.findAllByRoomId(any(), any())).thenReturn(listOf())
        whenever(chatRepository.findByRoomIdSize(any(), any(), any())).thenReturn(listOf())
        whenever(chatRepository.findByRoomIdAndChatId(any(), any(), any(), any())).thenReturn(listOf())
        whenever(chatRepository.findByImageChatSize(any(), any(), any())).thenReturn(listOf())
        whenever(chatRepository.findAllByIdIn(any(), any())).thenReturn(listOf())
    }

    private fun room(memberCount: Int) {
        val chatRoom = ChatRoom(ROOM, "room", "", "", "", "2026-09-13 00:00:00")
        for (i in 1..memberCount) {
            chatRoom.chatRoomMemberList.add(ChatRoomMember(i.toLong(), "", chatRoom))
        }
        whenever(chatRoomRepository.findByRoomId(ROOM)).thenReturn(Optional.of(chatRoom))
    }

    @Test
    @DisplayName("1:1 방이면 이력 질의 네 개 모두 차단 집합을 넘긴다")
    fun oneOnOneRoomPassesBlockedSet() {
        room(2)

        chatService.searchChatByRoomId(ROOM, "me")
        chatService.searchChatByRoomIdSize(ROOM, "30", "me")
        chatService.searchPrevChatByRoomId(ROOM, "100", "30", "me")
        chatService.searchImageChatByRoomIdSize(ROOM, "30", "me")

        verify(chatRepository).findAllByRoomId(ROOM, BLOCKED)
        verify(chatRepository).findByRoomIdSize(ROOM, 30L, BLOCKED)
        verify(chatRepository).findByRoomIdAndChatId(ROOM, 100L, 30L, BLOCKED)
        verify(chatRepository).findByImageChatSize(ROOM, 30L, BLOCKED)
    }

    @Test
    @DisplayName("단체방이면 제외 없이 부르고 차단 목록을 조회하지도 않는다")
    fun groupRoomIsNotFiltered() {
        room(3)

        chatService.searchChatByRoomIdSize(ROOM, "30", "me")

        verify(chatRepository).findByRoomIdSize(ROOM, 30L, setOf())
        verify(blockedIdsCache, never()).get(any())
    }

    @Test
    @DisplayName("X-Auth-User-Id 헤더가 없으면 필터하지 않는다")
    fun noHeaderMeansNoFilter() {
        room(2)

        chatService.searchChatByRoomIdSize(ROOM, "30", null)
        chatService.searchChatByRoomId(ROOM, "  ")

        verify(chatRepository).findByRoomIdSize(ROOM, 30L, setOf())
        verify(chatRepository).findAllByRoomId(ROOM, setOf())
        verify(blockedIdsCache, never()).get(any())
    }

    @Test
    @DisplayName("없는 방이면 1:1 로 보지 않아 필터가 걸리지 않는다")
    fun unknownRoomIsNotFiltered() {
        whenever(chatRoomRepository.findByRoomId(ROOM)).thenReturn(Optional.empty())

        chatService.searchChatByRoomIdSize(ROOM, "30", "me")

        verify(chatRepository).findByRoomIdSize(ROOM, 30L, setOf())
    }

    @Test
    @DisplayName("id 목록 조회는 방을 따지지 않고 차단 집합을 질의에 넘긴다(1:1 판정은 질의가 한다)")
    fun idLookupDelegatesRoomCheckToQuery() {
        chatService.searchChatListById(listOf("1", "2"), "me")

        verify(chatRepository).findAllByIdIn(eq(listOf(1L, 2L)), eq(BLOCKED))
        verify(chatRoomRepository, never()).findByRoomId(any())
    }

    @Test
    @DisplayName("id 목록 조회도 헤더가 없으면 빈 집합이다")
    fun idLookupWithoutHeader() {
        whenever(blockedIdsCache.get(null)).thenReturn(setOf())

        chatService.searchChatListById(listOf("1"), null)

        verify(chatRepository).findAllByIdIn(eq(listOf(1L)), eq(setOf()))
    }
}
