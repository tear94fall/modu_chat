package com.example.chatservice.application.usecase

import com.example.chatservice.application.common.exception.ErrorCode
import com.example.chatservice.application.domain.entity.ChatType
import com.example.chatservice.application.event.ChatEventPublisher
import com.example.chatservice.application.member.BlockedIdsCache
import com.example.chatservice.application.member.MemberGateway
import com.example.chatservice.application.support.ChatFixtures
import com.example.chatservice.application.support.assertErrorCode
import com.example.chatservice.application.support.stubMembers
import com.example.chatservice.application.support.userId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * 이력 조회의 차단 제외와 방 멤버 확인.
 * 단체방이면 차단 목록(member-service)을 아예 조회하지 않아야 한다(불필요한 왕복).
 */
@SpringBootTest
class ChatUseCaseBlockTest {

    companion object {
        private const val ROOM = "room-1"
        private const val GROUP = "room-group"
        private val ME = userId(1)
        private val BAD = userId(2)
    }

    @Autowired lateinit var chatUseCase: ChatUseCase
    @Autowired lateinit var fixtures: ChatFixtures
    @MockitoBean lateinit var memberGateway: MemberGateway
    @MockitoBean lateinit var chatEventPublisher: ChatEventPublisher
    @MockitoBean lateinit var blockedIdsCache: BlockedIdsCache

    private var lastBadChatId = 0L
    private var groupChatId = 0L

    @BeforeEach
    fun setUp() {
        fixtures.clear()
        memberGateway.stubMembers()
        whenever(blockedIdsCache.get(anyOrNull())).thenReturn(setOf())
        whenever(blockedIdsCache.get(ME)).thenReturn(setOf(BAD))

        val oneOnOne = fixtures.room(ROOM, 1L, 2L)
        fixtures.chat(oneOnOne, BAD, "b1")
        fixtures.chat(oneOnOne, ME, "m1")
        fixtures.chat(oneOnOne, BAD, "b2", ChatType.CHAT_TYPE_IMAGE)
        fixtures.chat(oneOnOne, ME, "m2", ChatType.CHAT_TYPE_IMAGE)
        lastBadChatId = fixtures.chat(oneOnOne, BAD, "b3").id!!

        val group = fixtures.room(GROUP, 1L, 2L, 3L)
        groupChatId = fixtures.chat(group, BAD, "group-bad").id!!
    }

    @AfterEach
    fun cleanUp() = fixtures.clear()

    @Test
    @DisplayName("1:1 방이면 이력 질의 네 개 모두 내가 차단한 사람의 메시지를 뺀다")
    fun oneOnOneRoomExcludesBlockedSenders() {
        assertThat(chatUseCase.history(ME, ROOM)).extracting<String> { it.message }.containsExactlyInAnyOrder("m1", "m2")
        assertThat(chatUseCase.recent(ME, ROOM, 30)).extracting<String> { it.message }.containsExactlyInAnyOrder("m1", "m2")
        assertThat(chatUseCase.before(ME, ROOM, lastBadChatId, 30)).extracting<String> { it.message }
            .containsExactlyInAnyOrder("m1", "m2")
        assertThat(chatUseCase.images(ME, ROOM, 30)).extracting<String> { it.message }.containsExactly("m2")
        // 상대는 나를 차단하지 않았으므로 전부 본다.
        assertThat(chatUseCase.history(BAD, ROOM)).hasSize(5)
    }

    @Test
    @DisplayName("단체방이면 제외 없이 읽고 차단 목록을 조회하지도 않는다")
    fun groupRoomIsNotFiltered() {
        assertThat(chatUseCase.recent(ME, GROUP, 30)).extracting<String> { it.message }.containsExactly("group-bad")

        verify(blockedIdsCache, never()).get(any())
    }

    @Test
    @DisplayName("X-Auth-User-Id 가 없으면 이력을 읽을 수 없다(403)")
    fun noHeaderIsForbidden() {
        assertErrorCode(ErrorCode.FORBIDDEN) { chatUseCase.recent(null, ROOM, 30) }
        assertErrorCode(ErrorCode.FORBIDDEN) { chatUseCase.history("  ", ROOM) }

        verify(blockedIdsCache, never()).get(any())
    }

    @Test
    @DisplayName("없는 방이면 빈 목록이고 차단 목록을 조회하지도 않는다")
    fun unknownRoomIsNotFiltered() {
        assertThat(chatUseCase.recent(ME, "no-room", 30)).isEmpty()
        assertThat(chatUseCase.count(ME, "no-room")).isZero()

        verify(blockedIdsCache, never()).get(any())
    }

    @Test
    @DisplayName("id 목록 조회는 1:1 방의 차단한 사람 메시지만 빼고 단체방 메시지는 남긴다(1:1 판정은 질의가 한다)")
    fun idLookupFiltersOnlyOneOnOneRooms() {
        val chats = chatUseCase.chats(ME, listOf(lastBadChatId, groupChatId))

        assertThat(chats).extracting<String> { it.message }.containsExactly("group-bad")
        verify(blockedIdsCache).get(ME)
    }

    @Test
    @DisplayName("id 목록 조회는 내가 멤버가 아닌 방의 메시지를 조용히 뺀다")
    fun idLookupDropsOtherRooms() {
        val outsider = userId(3)

        assertThat(chatUseCase.chats(outsider, listOf(lastBadChatId, groupChatId)))
            .extracting<String> { it.message }.containsExactly("group-bad")
        assertThat(chatUseCase.chats(userId(4), listOf(lastBadChatId, groupChatId))).isEmpty()
    }

    @Test
    @DisplayName("방 멤버가 아니면 그 방의 대화를 어떤 조회로도 읽을 수 없다(403)")
    fun outsiderCannotReadRoom() {
        val outsider = userId(3)

        assertErrorCode(ErrorCode.NOT_CHAT_ROOM_MEMBER) { chatUseCase.history(outsider, ROOM) }
        assertErrorCode(ErrorCode.NOT_CHAT_ROOM_MEMBER) { chatUseCase.recent(outsider, ROOM, 30) }
        assertErrorCode(ErrorCode.NOT_CHAT_ROOM_MEMBER) { chatUseCase.before(outsider, ROOM, lastBadChatId, 30) }
        assertErrorCode(ErrorCode.NOT_CHAT_ROOM_MEMBER) { chatUseCase.images(outsider, ROOM, 30) }
        assertErrorCode(ErrorCode.NOT_CHAT_ROOM_MEMBER) { chatUseCase.page(outsider, ROOM, PageRequest.of(0, 20)) }
        assertErrorCode(ErrorCode.NOT_CHAT_ROOM_MEMBER) { chatUseCase.search(outsider, ROOM, "m") }
        assertErrorCode(ErrorCode.NOT_CHAT_ROOM_MEMBER) { chatUseCase.count(outsider, ROOM) }
        assertErrorCode(ErrorCode.NOT_CHAT_ROOM_MEMBER) { chatUseCase.chat(outsider, lastBadChatId) }
        assertErrorCode(ErrorCode.NOT_CHAT_ROOM_MEMBER) { chatUseCase.chatInRoom(outsider, ROOM, lastBadChatId) }
        // 내부 호출(ws-service)은 멤버 확인 없이 읽는다.
        assertThat(chatUseCase.chatForInternal(lastBadChatId).message).isEqualTo("b3")
    }
}
