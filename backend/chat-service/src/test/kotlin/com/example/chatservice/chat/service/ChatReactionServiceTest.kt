package com.example.chatservice.chat.service

import com.example.chatservice.chat.dto.ChatDto
import com.example.chatservice.chat.entity.Chat
import com.example.chatservice.chat.entity.ChatReaction
import com.example.chatservice.chat.entity.ReactionEmoji
import com.example.chatservice.chat.repository.ChatReactionRepository
import com.example.chatservice.chat.repository.ChatRepository
import com.example.chatservice.common.exception.CustomException
import com.example.chatservice.common.exception.ErrorCode
import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ChatReactionServiceTest {

    companion object {
        private const val ROOM = "room-1"
        private const val CHAT = 10L
        private const val AUTHOR = "author"
        private const val ME = "me"
    }

    private lateinit var chatRepository: ChatRepository
    private lateinit var reactionRepository: ChatReactionRepository
    private lateinit var service: ChatReactionService

    @BeforeEach
    fun setUp() {
        chatRepository = mock()
        reactionRepository = mock()
        service = ChatReactionService(chatRepository, reactionRepository)
        whenever(chatRepository.findById(CHAT)).thenReturn(Optional.of(Chat("hi", ROOM, null, AUTHOR, "2026-09-21 10:00:00", 1)))
        whenever(reactionRepository.findAllByChatIdInOrderByIdAsc(any())).thenReturn(listOf())
    }

    @Test
    @DisplayName("처음 남기면 저장되고 added=true, 작성자가 결과에 실린다")
    fun firstReactionIsSaved() {
        whenever(reactionRepository.findByChatIdAndUserId(CHAT, ME)).thenReturn(Optional.empty())
        whenever(reactionRepository.findAllByChatIdInOrderByIdAsc(any()))
            .thenReturn(listOf(ChatReaction(CHAT, ROOM, ME, ReactionEmoji.LIKE)))

        val result = service.react(ROOM, CHAT, ME, "like")

        val saved = argumentCaptor<ChatReaction>()
        verify(reactionRepository).save(saved.capture())
        assertEquals(ReactionEmoji.LIKE, saved.firstValue.emoji)
        assertTrue(result.added)
        assertEquals("LIKE", result.emoji)
        assertEquals(AUTHOR, result.authorUserId)
        assertEquals(1, result.reactions!!.size)
        assertEquals(listOf(ME), result.reactions!![0].userIds)
    }

    @Test
    @DisplayName("같은 이모지를 다시 보내면 취소(added=false), 다른 이모지면 교체")
    fun sameEmojiRemovesAndOtherEmojiReplaces() {
        val existing = ChatReaction(CHAT, ROOM, ME, ReactionEmoji.LIKE)
        whenever(reactionRepository.findByChatIdAndUserId(CHAT, ME)).thenReturn(Optional.of(existing))

        val removed = service.react(ROOM, CHAT, ME, "LIKE")
        verify(reactionRepository).delete(existing)
        assertFalse(removed.added)
        assertNull(removed.emoji)

        val replaced = service.react(ROOM, CHAT, ME, "HEART")
        assertEquals(ReactionEmoji.HEART, existing.emoji)
        assertTrue(replaced.added)
        verify(reactionRepository, never()).save(any())
    }

    @Test
    @DisplayName("내 메시지, 다른 방의 메시지, 모르는 이모지는 거부한다")
    fun rejectsOwnChatWrongRoomAndUnknownEmoji() {
        val own = assertThrows(CustomException::class.java) { service.react(ROOM, CHAT, AUTHOR, "LIKE") }
        assertEquals(ErrorCode.CANNOT_REACT_OWN_CHAT, own.errorCode)

        val room = assertThrows(CustomException::class.java) { service.react("other-room", CHAT, ME, "LIKE") }
        assertEquals(ErrorCode.CHAT_NOT_FOUND_ERROR, room.errorCode)

        val emoji = assertThrows(CustomException::class.java) { service.react(ROOM, CHAT, ME, "🍕") }
        assertEquals(ErrorCode.INVALID_REACTION, emoji.errorCode)
        verify(reactionRepository, never()).save(any())
    }

    @Test
    @DisplayName("이력 한 페이지의 반응을 한 번의 질의로 이모지별로 묶어 채운다")
    fun withReactionsGroupsPerChatAndEmoji() {
        whenever(reactionRepository.findAllByChatIdInOrderByIdAsc(listOf(1L, 2L))).thenReturn(
            listOf(
                ChatReaction(1L, ROOM, "a", ReactionEmoji.LIKE),
                ChatReaction(1L, ROOM, "b", ReactionEmoji.HEART),
                ChatReaction(1L, ROOM, "c", ReactionEmoji.LIKE),
            ),
        )
        val first = ChatDto(id = 1L)
        val second = ChatDto(id = 2L)

        service.withReactions(listOf(first, second))

        assertEquals(2, first.reactions!!.size)
        assertEquals("LIKE", first.reactions!![0].emoji)
        assertEquals(2, first.reactions!![0].count)
        assertEquals(listOf("a", "c"), first.reactions!![0].userIds)
        assertEquals("HEART", first.reactions!![1].emoji)
        assertTrue(second.reactions!!.isEmpty())
        verify(reactionRepository, never()).findAllByChatIdInOrderByIdAsc(listOf(1L))
    }
}
