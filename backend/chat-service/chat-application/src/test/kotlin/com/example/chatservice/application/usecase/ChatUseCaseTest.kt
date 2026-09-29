package com.example.chatservice.application.usecase

import com.example.chatservice.application.common.exception.ErrorCode
import com.example.chatservice.application.domain.entity.ReactionEmoji
import com.example.chatservice.application.event.ChatEventPublisher
import com.example.chatservice.application.member.BlockedIdsCache
import com.example.chatservice.application.member.MemberGateway
import com.example.chatservice.application.message.ChatMessage
import com.example.chatservice.application.message.SubscribeType
import com.example.chatservice.application.support.ChatFixtures
import com.example.chatservice.application.support.assertErrorCode
import com.example.chatservice.application.support.stubMembers
import com.example.chatservice.application.support.userId
import com.example.chatservice.application.usecase.command.SaveChatCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

/** 메시지 저장·단건 조회·삭제와 Kafka 릴레이. */
@SpringBootTest
class ChatUseCaseTest {

    @Autowired lateinit var chatUseCase: ChatUseCase
    @Autowired lateinit var chatRoomUseCase: ChatRoomUseCase
    @Autowired lateinit var chatRelayUseCase: ChatRelayUseCase
    @Autowired lateinit var fixtures: ChatFixtures
    @MockitoBean lateinit var memberGateway: MemberGateway
    @MockitoBean lateinit var chatEventPublisher: ChatEventPublisher
    @MockitoBean lateinit var blockedIdsCache: BlockedIdsCache

    @BeforeEach
    fun setUp() {
        fixtures.clear()
        memberGateway.stubMembers()
        whenever(blockedIdsCache.get(anyOrNull())).thenReturn(setOf())
    }

    @AfterEach
    fun cleanUp() = fixtures.clear()

    private fun save(roomId: String, sender: String, message: String, chatTime: String = "2026-09-20 11:00:00"): Long =
        chatUseCase.save(SaveChatCommand(chatType = 1, roomId = roomId, sender = sender, message = message, chatTime = chatTime))!!

    @Test
    @DisplayName("메시지를 저장하면 방의 마지막 메시지가 바뀌고, 보낸 쪽이 찍은 UTC 시각은 그대로 남는다")
    fun save_updatesLastChat_andKeepsChatTime() {
        fixtures.room("room-1", 1L, 2L)

        val first = save("room-1", userId(1), "첫 번째", "2026-09-20 02:03:04")
        val second = save("room-1", userId(2), "두 번째", "2026-09-20 02:03:05")

        assertThat(second).isGreaterThan(first)
        assertThat(chatUseCase.chatForInternal(first).chatTime).isEqualTo("2026-09-20 02:03:04")
        val room = chatRoomUseCase.roomForInternal("room-1").room
        assertThat(room.lastChatId).isEqualTo(second.toString())
        assertThat(room.lastChatMsg).isEqualTo("두 번째")
        // 저장 순서가 곧 id 순서다.
        assertThat(chatUseCase.history(userId(1), "room-1")).extracting<Long> { it.id }.containsExactly(first, second)
        assertErrorCode(ErrorCode.CHATROOM_NOT_FOUND_ERROR) { save("no-room", userId(1), "x") }
    }

    @Test
    @DisplayName("방 id 와 메시지 id 로 메시지를 찾는다(고친 버그: 인자 순서가 뒤바뀌어 늘 500 이었다)")
    fun chatInRoom_returnsTheMessage() {
        val room = fixtures.room("3f2b9c1e-8a7d-4e21-9b0a-2c6d5e4f1a3b", 1L, 2L)
        val other = fixtures.room("room-other", 1L, 2L)
        val chat = fixtures.chat(room, userId(2), "안녕")
        fixtures.reaction(chat, userId(1), ReactionEmoji.HEART)

        val found = chatUseCase.chatInRoom(userId(1), room.roomId!!, chat.id!!)

        assertThat(found!!.message).isEqualTo("안녕")
        assertThat(found.reactions).extracting<String> { it.emoji }.containsExactly("HEART")
        // 다른 방의 메시지 id 거나 없는 id 면 없다.
        assertThat(chatUseCase.chatInRoom(userId(1), other.roomId!!, chat.id!!)).isNull()
        assertThat(chatUseCase.chatInRoom(userId(1), room.roomId!!, Long.MAX_VALUE)).isNull()
    }

    @Test
    @DisplayName("보낸 사람만 자기 메시지를 지울 수 있다(403). 지우면 반응도 지워지고 방의 마지막 메시지가 바뀐다")
    fun delete_onlyBySender() {
        fixtures.room("room-1", 1L, 2L)
        val first = save("room-1", userId(1), "남는 메시지")
        val mine = save("room-1", userId(1), "지울 메시지")
        val reactionTarget = chatUseCase.chatForInternal(mine)

        assertErrorCode(ErrorCode.NOT_CHAT_SENDER) { chatUseCase.delete(userId(2), "room-1", mine) }
        assertErrorCode(ErrorCode.FORBIDDEN) { chatUseCase.delete(null, "room-1", mine) }
        assertThat(chatUseCase.chatForInternal(mine).message).isEqualTo("지울 메시지")

        val deleted = chatUseCase.delete(userId(1), "room-1", mine)

        assertThat(deleted.id).isEqualTo(reactionTarget.id)
        assertThat(deleted.message).isEqualTo("지울 메시지")
        assertErrorCode(ErrorCode.CHAT_NOT_FOUND_ERROR) { chatUseCase.chatForInternal(mine) }
        val room = chatRoomUseCase.roomForInternal("room-1").room
        assertThat(room.lastChatId).isEqualTo(first.toString())
        assertThat(room.lastChatMsg).isEqualTo("삭제된 메시지 입니다.")
    }

    @Test
    @DisplayName("없는 방·없는 메시지·다른 방의 메시지는 지울 수 없다(404)")
    fun delete_unknownTargets() {
        fixtures.room("room-1", 1L, 2L)
        fixtures.room("room-2", 1L, 3L)
        val mine = save("room-1", userId(1), "내 메시지")

        assertErrorCode(ErrorCode.CHATROOM_NOT_FOUND_ERROR) { chatUseCase.delete(userId(1), "no-room", mine) }
        assertErrorCode(ErrorCode.CHAT_NOT_FOUND_ERROR) { chatUseCase.delete(userId(1), "room-1", Long.MAX_VALUE) }
        assertErrorCode(ErrorCode.CHAT_NOT_FOUND_ERROR) { chatUseCase.delete(userId(1), "room-2", mine) }
    }

    @Test
    @DisplayName("마지막 남은 메시지를 지우면 방의 마지막 메시지 id 는 비어 있다")
    fun delete_lastRemainingChat() {
        fixtures.room("room-1", 1L, 2L)
        val only = save("room-1", userId(1), "하나뿐")
        fixtures.reaction(fixtures.chat(fixtures.room("room-2", 1L), userId(1), "다른 방"), userId(2), ReactionEmoji.LIKE)

        chatUseCase.delete(userId(1), "room-1", only)

        assertThat(chatRoomUseCase.roomForInternal("room-1").room.lastChatId).isEmpty()
        assertThat(chatUseCase.history(userId(1), "room-2")[0].reactions).hasSize(1)
    }

    @Test
    @DisplayName("릴레이는 받은 메시지를 방 id 를 key 로 그대로 넘긴다(excludeUserIds 포함)")
    fun relay_passesTheSameMessage() {
        val message = ChatMessage(SubscribeType.BROAD_CAST, "room-1", "10", listOf("blocked-user"))

        chatRelayUseCase.relay(message)

        verify(chatEventPublisher).broadcast("room-1", message)
    }
}
