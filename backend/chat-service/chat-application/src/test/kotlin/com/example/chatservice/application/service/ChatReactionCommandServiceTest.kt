package com.example.chatservice.application.service

import com.example.chatservice.application.common.exception.ErrorCode
import com.example.chatservice.application.domain.entity.Chat
import com.example.chatservice.application.domain.entity.ReactionEmoji
import com.example.chatservice.application.event.ChatEventPublisher
import com.example.chatservice.application.member.MemberGateway
import com.example.chatservice.application.support.ChatFixtures
import com.example.chatservice.application.support.assertErrorCode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
class ChatReactionCommandServiceTest {

    companion object {
        private const val ROOM = "room-1"
        private const val AUTHOR = "author"
        private const val ME = "me"
    }

    @Autowired lateinit var service: ChatReactionCommandService
    @Autowired lateinit var chatCommandService: ChatCommandService
    @Autowired lateinit var chatQueryService: ChatQueryService
    @Autowired lateinit var fixtures: ChatFixtures
    @MockitoBean lateinit var memberGateway: MemberGateway
    @MockitoBean lateinit var chatEventPublisher: ChatEventPublisher

    private lateinit var chat: Chat

    @BeforeEach
    fun setUp() {
        fixtures.clear()
        chat = fixtures.chat(fixtures.room(ROOM, 1L, 2L), AUTHOR, "hi")
    }

    @AfterEach
    fun cleanUp() = fixtures.clear()

    @Test
    @DisplayName("처음 남기면 저장되고 added=true, 작성자가 결과에 실린다")
    fun firstReactionIsSaved() {
        val result = service.react(ROOM, chat.id!!, ME, "like")

        assertTrue(result.added)
        assertEquals("LIKE", result.emoji)
        assertEquals(AUTHOR, result.authorUserId)
        assertEquals(1, result.reactions!!.size)
        assertEquals(listOf(ME), result.reactions!![0].userIds)
        // 반응 직후의 메시지 조회(master)에 바로 보인다.
        assertEquals("LIKE", chatCommandService.findById(chat.id!!).reactions!![0].emoji)
    }

    @Test
    @DisplayName("같은 이모지를 다시 보내면 취소(added=false), 다른 이모지면 교체")
    fun sameEmojiRemovesAndOtherEmojiReplaces() {
        service.react(ROOM, chat.id!!, ME, "LIKE")

        val removed = service.react(ROOM, chat.id!!, ME, "LIKE")
        assertFalse(removed.added)
        assertNull(removed.emoji)
        assertTrue(removed.reactions!!.isEmpty())

        service.react(ROOM, chat.id!!, ME, "LIKE")
        val replaced = service.react(ROOM, chat.id!!, ME, "HEART")
        assertTrue(replaced.added)
        assertEquals("HEART", replaced.emoji)
        assertEquals(listOf("HEART"), replaced.reactions!!.map { it.emoji })
        assertEquals(1, replaced.reactions!![0].count)
    }

    @Test
    @DisplayName("내 메시지, 다른 방의 메시지, 모르는 이모지는 거부한다")
    fun rejectsOwnChatWrongRoomAndUnknownEmoji() {
        assertErrorCode(ErrorCode.CANNOT_REACT_OWN_CHAT) { service.react(ROOM, chat.id!!, AUTHOR, "LIKE") }
        assertErrorCode(ErrorCode.CHAT_NOT_FOUND_ERROR) { service.react("other-room", chat.id!!, ME, "LIKE") }
        assertErrorCode(ErrorCode.CHAT_NOT_FOUND_ERROR) { service.react(ROOM, Long.MAX_VALUE, ME, "LIKE") }
        assertErrorCode(ErrorCode.INVALID_REACTION) { service.react(ROOM, chat.id!!, ME, "🍕") }

        assertTrue(chatCommandService.findById(chat.id!!).reactions!!.isEmpty())
    }

    @Test
    @DisplayName("이력 한 페이지의 반응을 이모지별로 묶어 채운다. 처음 남겨진 이모지가 앞에 온다")
    fun withReactionsGroupsPerChatAndEmoji() {
        val second = fixtures.chat(fixtures.room("room-2", 1L, 2L), AUTHOR, "second")
        fixtures.reaction(chat, "a", ReactionEmoji.LIKE)
        fixtures.reaction(chat, "b", ReactionEmoji.HEART)
        fixtures.reaction(chat, "c", ReactionEmoji.LIKE)

        for (chats in listOf(
            chatCommandService.findAllByIds(listOf(chat.id!!, second.id!!), emptySet()),
            chatQueryService.findAllByRoom(ROOM, emptySet()) + chatQueryService.findAllByRoom("room-2", emptySet()),
        )) {
            val first = chats.first { it.id == chat.id }
            assertEquals(2, first.reactions!!.size)
            assertEquals("LIKE", first.reactions!![0].emoji)
            assertEquals(2, first.reactions!![0].count)
            assertEquals(listOf("a", "c"), first.reactions!![0].userIds)
            assertEquals("HEART", first.reactions!![1].emoji)
            assertTrue(chats.first { it.id == second.id }.reactions!!.isEmpty())
        }
    }
}
