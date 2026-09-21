package com.example.chatservice.chat.service;

import com.example.chatservice.chat.dto.ChatDto;
import com.example.chatservice.chat.dto.ReactionResultDto;
import com.example.chatservice.chat.entity.Chat;
import com.example.chatservice.chat.entity.ChatReaction;
import com.example.chatservice.chat.entity.ReactionEmoji;
import com.example.chatservice.chat.repository.ChatReactionRepository;
import com.example.chatservice.chat.repository.ChatRepository;
import com.example.chatservice.common.exception.CustomException;
import com.example.chatservice.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatReactionServiceTest {

    private static final String ROOM = "room-1";
    private static final long CHAT = 10L;
    private static final String AUTHOR = "author";
    private static final String ME = "me";

    private ChatRepository chatRepository;
    private ChatReactionRepository reactionRepository;
    private ChatReactionService service;

    @BeforeEach
    void setUp() {
        chatRepository = mock(ChatRepository.class);
        reactionRepository = mock(ChatReactionRepository.class);
        service = new ChatReactionService(chatRepository, reactionRepository);
        when(chatRepository.findById(CHAT)).thenReturn(Optional.of(new Chat("hi", ROOM, null, AUTHOR, "2026-09-21 10:00:00", 1)));
        when(reactionRepository.findAllByChatIdInOrderByIdAsc(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("처음 남기면 저장되고 added=true, 작성자가 결과에 실린다")
    void firstReactionIsSaved() {
        when(reactionRepository.findByChatIdAndUserId(CHAT, ME)).thenReturn(Optional.empty());
        when(reactionRepository.findAllByChatIdInOrderByIdAsc(any()))
                .thenReturn(List.of(new ChatReaction(CHAT, ROOM, ME, ReactionEmoji.LIKE)));

        ReactionResultDto result = service.react(ROOM, CHAT, ME, "like");

        ArgumentCaptor<ChatReaction> saved = ArgumentCaptor.forClass(ChatReaction.class);
        verify(reactionRepository).save(saved.capture());
        assertEquals(ReactionEmoji.LIKE, saved.getValue().getEmoji());
        assertTrue(result.isAdded());
        assertEquals("LIKE", result.getEmoji());
        assertEquals(AUTHOR, result.getAuthorUserId());
        assertEquals(1, result.getReactions().size());
        assertEquals(List.of(ME), result.getReactions().get(0).getUserIds());
    }

    @Test
    @DisplayName("같은 이모지를 다시 보내면 취소(added=false), 다른 이모지면 교체")
    void sameEmojiRemovesAndOtherEmojiReplaces() {
        ChatReaction existing = new ChatReaction(CHAT, ROOM, ME, ReactionEmoji.LIKE);
        when(reactionRepository.findByChatIdAndUserId(CHAT, ME)).thenReturn(Optional.of(existing));

        ReactionResultDto removed = service.react(ROOM, CHAT, ME, "LIKE");
        verify(reactionRepository).delete(existing);
        assertFalse(removed.isAdded());
        assertNull(removed.getEmoji());

        ReactionResultDto replaced = service.react(ROOM, CHAT, ME, "HEART");
        assertEquals(ReactionEmoji.HEART, existing.getEmoji());
        assertTrue(replaced.isAdded());
        verify(reactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("내 메시지, 다른 방의 메시지, 모르는 이모지는 거부한다")
    void rejectsOwnChatWrongRoomAndUnknownEmoji() {
        CustomException own = assertThrows(CustomException.class, () -> service.react(ROOM, CHAT, AUTHOR, "LIKE"));
        assertEquals(ErrorCode.CANNOT_REACT_OWN_CHAT, own.getErrorCode());

        CustomException room = assertThrows(CustomException.class, () -> service.react("other-room", CHAT, ME, "LIKE"));
        assertEquals(ErrorCode.CHAT_NOT_FOUND_ERROR, room.getErrorCode());

        CustomException emoji = assertThrows(CustomException.class, () -> service.react(ROOM, CHAT, ME, "🍕"));
        assertEquals(ErrorCode.INVALID_REACTION, emoji.getErrorCode());
        verify(reactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("이력 한 페이지의 반응을 한 번의 질의로 이모지별로 묶어 채운다")
    void withReactionsGroupsPerChatAndEmoji() {
        when(reactionRepository.findAllByChatIdInOrderByIdAsc(List.of(1L, 2L))).thenReturn(List.of(
                new ChatReaction(1L, ROOM, "a", ReactionEmoji.LIKE),
                new ChatReaction(1L, ROOM, "b", ReactionEmoji.HEART),
                new ChatReaction(1L, ROOM, "c", ReactionEmoji.LIKE)));
        ChatDto first = ChatDto.builder().id(1L).build();
        ChatDto second = ChatDto.builder().id(2L).build();

        service.withReactions(List.of(first, second));

        assertEquals(2, first.getReactions().size());
        assertEquals("LIKE", first.getReactions().get(0).getEmoji());
        assertEquals(2, first.getReactions().get(0).getCount());
        assertEquals(List.of("a", "c"), first.getReactions().get(0).getUserIds());
        assertEquals("HEART", first.getReactions().get(1).getEmoji());
        assertTrue(second.getReactions().isEmpty());
        verify(reactionRepository, never()).findAllByChatIdInOrderByIdAsc(List.of(1L));
    }
}
