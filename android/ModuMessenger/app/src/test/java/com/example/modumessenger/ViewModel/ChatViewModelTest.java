package com.example.modumessenger.ViewModel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.modumessenger.Repository.ChatRepository;
import com.example.modumessenger.dto.ChatDto;

import org.junit.Before;
import org.junit.Test;

public class ChatViewModelTest {

    private static final String ROOM_ID = "room-1";
    private static final String ME = "me";
    private static final String OTHER = "someone-else";

    private ChatRepository repository;
    private ChatViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(ChatRepository.class);
        viewModel = new ChatViewModel(repository, ROOM_ID, ME);
    }

    @Test
    public void consumePendingScrollToBottom_isFalseBeforeAnySend() {
        assertFalse(viewModel.consumePendingScrollToBottom());
    }

    @Test
    public void sendText_setsPendingScrollToBottom_evenWhenSendFails() {
        when(repository.sendChat(any(ChatDto.class))).thenReturn(false);

        viewModel.sendText("hello");

        // 실패해도 재전송/삭제 가능한 실패 말풍선이 하나 추가되므로 바닥으로 내려야 한다.
        assertTrue(viewModel.consumePendingScrollToBottom());
    }

    @Test
    public void sendImage_setsPendingScrollToBottom() {
        when(repository.sendChat(any(ChatDto.class))).thenReturn(true);

        viewModel.sendImage("photo.png");

        assertTrue(viewModel.consumePendingScrollToBottom());
    }

    @Test
    public void consumePendingScrollToBottom_resetsAfterBeingRead() {
        when(repository.sendChat(any(ChatDto.class))).thenReturn(true);

        viewModel.sendText("hello");
        assertTrue(viewModel.consumePendingScrollToBottom());

        // 한 번 읽었으면 다음 갱신에서 또 스크롤을 강제하면 안 된다.
        assertFalse(viewModel.consumePendingScrollToBottom());
    }

    @Test
    public void shouldShowJumpToBottom_falseWhenAlreadyAtBottom() {
        assertFalse(ChatViewModel.shouldShowJumpToBottom(true, OTHER, ME));
    }

    @Test
    public void shouldShowJumpToBottom_falseForMyOwnMessage() {
        assertFalse(ChatViewModel.shouldShowJumpToBottom(false, ME, ME));
    }

    @Test
    public void shouldShowJumpToBottom_falseWhenSenderUnknown() {
        assertFalse(ChatViewModel.shouldShowJumpToBottom(false, null, ME));
    }

    @Test
    public void shouldShowJumpToBottom_trueForOthersMessageWhenNotAtBottom() {
        assertTrue(ChatViewModel.shouldShowJumpToBottom(false, OTHER, ME));
    }
}
