package com.example.chatservice.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.chatservice.chat.dto.ChatRoomLastReadChatDto;
import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.ChatRoomMember;
import com.example.chatservice.chat.repository.ChatRepository;
import com.example.chatservice.chat.repository.ChatRoomMemberRepository;
import com.example.chatservice.chat.repository.ChatRoomRepository;
import com.example.chatservice.kafka.producer.KafkaProducerService;
import com.example.chatservice.member.client.MemberFeignClient;
import com.example.chatservice.member.dto.MemberDto;
import com.example.chatservice.member.service.BlockedIdsCache;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

/**
 * 미읽음 개수의 차단 제외.
 *
 * 경로 변수 이름은 {userId} 지만 값은 member id(숫자 PK) 다 — 안드로이드가 myMemberId 를
 * 넣는다. 차단 조회는 userId(구글 sub) 기준이라 서비스가 한 번 변환해야 하고,
 * 그 변환이 실패하면 게이트웨이가 넣어 준 X-Auth-User-Id 로 떨어진다. 여기서 그 세 갈래를 본다.
 */
class ChatRoomUnreadBlockTest {

    private static final String MEMBER_ID = "7";
    private static final String MY_USER_ID = "google-sub-7";
    private static final Set<String> BLOCKED = Set.of("bad");

    private ChatRoomMemberRepository chatRoomMemberRepository;
    private ChatRepository chatRepository;
    private MemberFeignClient memberFeignClient;
    private BlockedIdsCache blockedIdsCache;
    private ChatRoomService chatRoomService;

    @BeforeEach
    void setUp() {
        chatRoomMemberRepository = mock(ChatRoomMemberRepository.class);
        chatRepository = mock(ChatRepository.class);
        memberFeignClient = mock(MemberFeignClient.class);
        blockedIdsCache = mock(BlockedIdsCache.class);

        chatRoomService = new ChatRoomService(
                chatRoomMemberRepository,
                mock(ChatRoomRepository.class),
                chatRepository,
                memberFeignClient,
                new ModelMapper(),
                mock(KafkaProducerService.class),
                blockedIdsCache);

        when(memberFeignClient.getMembersById(anyList()))
                .thenReturn(List.of(MemberDto.builder().id(7L).userId(MY_USER_ID).build()));
        when(blockedIdsCache.get(any())).thenReturn(Set.of());
        when(blockedIdsCache.get(MY_USER_ID)).thenReturn(BLOCKED);
        when(chatRepository.findAllByIdIn(anyList())).thenReturn(List.of());
        when(chatRepository.countByRoomIdAndIdBetween(anyString(), anyLong(), anyLong(), any())).thenReturn(4L);
    }

    /** 멤버 수만큼 사람이 있고 마지막 메시지가 10번, 내가 읽은 것은 5번인 방. */
    private void roomWith(int memberCount) {
        ChatRoom chatRoom = new ChatRoom("room-1", "room", "", "", "10", "2026-09-13 00:00:00");
        ChatRoomMember me = new ChatRoomMember(7L, "5", chatRoom);
        chatRoom.getChatRoomMemberList().add(me);
        for (long i = 1; i < memberCount; i++) {
            chatRoom.getChatRoomMemberList().add(new ChatRoomMember(100 + i, "5", chatRoom));
        }
        when(chatRoomMemberRepository.findAllByMemberId(7L)).thenReturn(List.of(me));
    }

    @Test
    @DisplayName("1:1 방은 member id 를 userId 로 바꿔 조회한 차단 목록을 개수 질의에 넘긴다")
    void oneOnOneUsesResolvedUserId() {
        roomWith(2);

        List<ChatRoomLastReadChatDto> result = chatRoomService.searchUnreadChatRoom(MEMBER_ID, null);

        verify(blockedIdsCache).get(MY_USER_ID);
        verify(chatRepository).countByRoomIdAndIdBetween("room-1", 6L, 10L, BLOCKED);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUnreadChatCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("단체방은 제외 없이 센다")
    void groupRoomIsNotFiltered() {
        roomWith(3);

        chatRoomService.searchUnreadChatRoom(MEMBER_ID, null);

        verify(chatRepository).countByRoomIdAndIdBetween("room-1", 6L, 10L, Set.of());
    }

    @Test
    @DisplayName("member-service 로 userId 변환이 안 되면 X-Auth-User-Id 로 차단 목록을 찾는다")
    void fallsBackToAuthHeaderWhenResolutionFails() {
        roomWith(2);
        when(memberFeignClient.getMembersById(anyList())).thenThrow(new RuntimeException("member-service down"));
        when(blockedIdsCache.get("header-user")).thenReturn(Set.of("bad2"));

        chatRoomService.searchUnreadChatRoom(MEMBER_ID, "header-user");

        verify(chatRepository).countByRoomIdAndIdBetween("room-1", 6L, 10L, Set.of("bad2"));
    }

    @Test
    @DisplayName("신원을 전혀 못 찾으면 예전처럼 제외 없이 센다")
    void noIdentityMeansNoFilter() {
        roomWith(2);
        when(memberFeignClient.getMembersById(anyList())).thenReturn(List.of());

        chatRoomService.searchUnreadChatRoom(MEMBER_ID, null);

        verify(chatRepository).countByRoomIdAndIdBetween(eq("room-1"), eq(6L), eq(10L), eq(Set.of()));
    }
}
