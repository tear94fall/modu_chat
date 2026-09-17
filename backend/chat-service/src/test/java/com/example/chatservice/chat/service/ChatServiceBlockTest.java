package com.example.chatservice.chat.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.ChatRoomMember;
import com.example.chatservice.chat.repository.ChatRepository;
import com.example.chatservice.chat.repository.ChatRoomRepository;
import com.example.chatservice.member.service.BlockedIdsCache;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

/**
 * 이력 조회가 어떤 제외 집합으로 저장소를 부르는지 본다.
 * 단체방·헤더 없음이면 member-service 를 아예 부르지 않아야 한다(불필요한 왕복).
 */
class ChatServiceBlockTest {

    private static final String ROOM = "room-1";
    private static final Set<String> BLOCKED = Set.of("bad");

    private ChatRoomRepository chatRoomRepository;
    private ChatRepository chatRepository;
    private BlockedIdsCache blockedIdsCache;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatRoomRepository = mock(ChatRoomRepository.class);
        chatRepository = mock(ChatRepository.class);
        blockedIdsCache = mock(BlockedIdsCache.class);
        chatService = new ChatService(chatRoomRepository, chatRepository, new ModelMapper(), blockedIdsCache);

        when(blockedIdsCache.get(anyString())).thenReturn(BLOCKED);
        when(chatRepository.findAllByRoomId(anyString(), any())).thenReturn(List.of());
        when(chatRepository.findByRoomIdSize(anyString(), anyLong(), any())).thenReturn(List.of());
        when(chatRepository.findByRoomIdAndChatId(anyString(), anyLong(), anyLong(), any())).thenReturn(List.of());
        when(chatRepository.findByImageChatSize(anyString(), anyLong(), any())).thenReturn(List.of());
        when(chatRepository.findAllByIdIn(any(), any())).thenReturn(List.of());
    }

    private void room(int memberCount) {
        ChatRoom chatRoom = new ChatRoom(ROOM, "room", "", "", "", "2026-09-13 00:00:00");
        for (long i = 1; i <= memberCount; i++) {
            chatRoom.getChatRoomMemberList().add(new ChatRoomMember(i, "", chatRoom));
        }
        when(chatRoomRepository.findByRoomId(ROOM)).thenReturn(Optional.of(chatRoom));
    }

    @Test
    @DisplayName("1:1 방이면 이력 질의 네 개 모두 차단 집합을 넘긴다")
    void oneOnOneRoomPassesBlockedSet() {
        room(2);

        chatService.searchChatByRoomId(ROOM, "me");
        chatService.searchChatByRoomIdSize(ROOM, "30", "me");
        chatService.searchPrevChatByRoomId(ROOM, "100", "30", "me");
        chatService.searchImageChatByRoomIdSize(ROOM, "30", "me");

        verify(chatRepository).findAllByRoomId(ROOM, BLOCKED);
        verify(chatRepository).findByRoomIdSize(ROOM, 30L, BLOCKED);
        verify(chatRepository).findByRoomIdAndChatId(ROOM, 100L, 30L, BLOCKED);
        verify(chatRepository).findByImageChatSize(ROOM, 30L, BLOCKED);
    }

    @Test
    @DisplayName("단체방이면 제외 없이 부르고 차단 목록을 조회하지도 않는다")
    void groupRoomIsNotFiltered() {
        room(3);

        chatService.searchChatByRoomIdSize(ROOM, "30", "me");

        verify(chatRepository).findByRoomIdSize(ROOM, 30L, Set.of());
        verify(blockedIdsCache, never()).get(anyString());
    }

    @Test
    @DisplayName("X-Auth-User-Id 헤더가 없으면 필터하지 않는다")
    void noHeaderMeansNoFilter() {
        room(2);

        chatService.searchChatByRoomIdSize(ROOM, "30", null);
        chatService.searchChatByRoomId(ROOM, "  ");

        verify(chatRepository).findByRoomIdSize(ROOM, 30L, Set.of());
        verify(chatRepository).findAllByRoomId(ROOM, Set.of());
        verify(blockedIdsCache, never()).get(anyString());
    }

    @Test
    @DisplayName("없는 방이면 1:1 로 보지 않아 필터가 걸리지 않는다")
    void unknownRoomIsNotFiltered() {
        when(chatRoomRepository.findByRoomId(ROOM)).thenReturn(Optional.empty());

        chatService.searchChatByRoomIdSize(ROOM, "30", "me");

        verify(chatRepository).findByRoomIdSize(ROOM, 30L, Set.of());
    }

    @Test
    @DisplayName("id 목록 조회는 방을 따지지 않고 차단 집합을 질의에 넘긴다(1:1 판정은 질의가 한다)")
    void idLookupDelegatesRoomCheckToQuery() {
        chatService.searchChatListById(List.of("1", "2"), "me");

        verify(chatRepository).findAllByIdIn(eq(List.of(1L, 2L)), eq(BLOCKED));
        verify(chatRoomRepository, never()).findByRoomId(anyString());
    }

    @Test
    @DisplayName("id 목록 조회도 헤더가 없으면 빈 집합이다")
    void idLookupWithoutHeader() {
        when(blockedIdsCache.get(null)).thenReturn(Set.of());

        chatService.searchChatListById(List.of("1"), null);

        verify(chatRepository).findAllByIdIn(eq(List.of(1L)), eq(Set.of()));
    }
}
