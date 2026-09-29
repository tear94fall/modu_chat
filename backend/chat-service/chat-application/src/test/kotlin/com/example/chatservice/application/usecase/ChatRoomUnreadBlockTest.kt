package com.example.chatservice.application.usecase

import com.example.chatservice.application.common.exception.ErrorCode
import com.example.chatservice.application.domain.entity.ChatRoom
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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * 미읽음 개수의 차단 제외와 본인 확인.
 *
 * 경로 변수 이름은 {userId} 지만 안드로이드는 회원 id(숫자 PK)를, iOS 는 userId 를 넣는다. 어느 쪽이든
 * 게이트웨이가 넣어 준 X-Auth-User-Id 의 주인이어야 하고, 차단 목록은 그 userId 로 찾는다.
 */
@SpringBootTest
class ChatRoomUnreadBlockTest {

    companion object {
        private val ME = userId(7)
        private val BAD = userId(8)
    }

    @Autowired lateinit var chatRoomUseCase: ChatRoomUseCase
    @Autowired lateinit var fixtures: ChatFixtures
    @MockitoBean lateinit var memberGateway: MemberGateway
    @MockitoBean lateinit var chatEventPublisher: ChatEventPublisher
    @MockitoBean lateinit var blockedIdsCache: BlockedIdsCache

    @BeforeEach
    fun setUp() {
        fixtures.clear()
        memberGateway.stubMembers()
        whenever(blockedIdsCache.get(anyOrNull())).thenReturn(setOf())
        whenever(blockedIdsCache.get(ME)).thenReturn(setOf(BAD))
    }

    @AfterEach
    fun cleanUp() = fixtures.clear()

    /** 내가 첫 메시지까지 읽었고 그 뒤로 BAD 가 둘, 다른 사람이 둘 보낸 방. 마지막 메시지는 다른 사람 것이다. */
    private fun roomWith(roomId: String, vararg memberIds: Long): ChatRoom {
        val room = fixtures.room(roomId, *memberIds)
        val other = userId(memberIds.last())
        val read = fixtures.chat(room, other, "읽은 메시지")
        fixtures.chat(room, BAD, "b1")
        fixtures.chat(room, other, "o1")
        fixtures.chat(room, BAD, "b2")
        val last = fixtures.chat(room, other, "o2")
        fixtures.lastChat(roomId, last.id!!, read.id!!)
        return room
    }

    @Test
    @DisplayName("1:1 방은 내가 차단한 사람의 메시지를 빼고 센다")
    fun oneOnOneExcludesBlockedSenders() {
        roomWith("room-1", 7L, 9L)

        val result = chatRoomUseCase.unread(ME, "7")

        assertThat(result).hasSize(1)
        assertThat(result[0].roomId).isEqualTo("room-1")
        assertThat(result[0].unreadChatCount).isEqualTo(2L)
        assertThat(result[0].lastSendChatId - result[0].lastReadChatId).isEqualTo(4L)
    }

    @Test
    @DisplayName("단체방은 제외 없이 센다")
    fun groupRoomIsNotFiltered() {
        roomWith("room-group", 7L, 8L, 9L)

        assertThat(chatRoomUseCase.unread(ME, "7")[0].unreadChatCount).isEqualTo(4L)
    }

    @Test
    @DisplayName("회원 id(안드로이드)로도, userId(iOS)로도 같은 답이 나오고 마지막 메시지를 내가 보냈으면 0 이다")
    fun acceptsMemberIdAndUserId() {
        val room = roomWith("room-1", 7L, 9L)

        assertThat(chatRoomUseCase.unread(ME, ME)).isEqualTo(chatRoomUseCase.unread(ME, "7"))

        val mine = fixtures.chat(room, ME, "내가 보낸 마지막 메시지")
        fixtures.lastChat("room-1", mine.id!!, 0L)
        assertThat(chatRoomUseCase.unread(ME, ME)[0].unreadChatCount).isZero()
    }

    @Test
    @DisplayName("남의 안 읽은 개수는 볼 수 없다(403)")
    fun someoneElseIsForbidden() {
        roomWith("room-1", 7L, 9L)

        assertErrorCode(ErrorCode.FORBIDDEN) { chatRoomUseCase.unread(ME, "9") }
        assertErrorCode(ErrorCode.FORBIDDEN) { chatRoomUseCase.unread(ME, userId(9)) }
        assertErrorCode(ErrorCode.FORBIDDEN) { chatRoomUseCase.unread(null, "7") }
    }
}
