package com.example.chatservice.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.ChatRoomMember;
import com.example.chatservice.chat.repository.ChatRepository;
import com.example.chatservice.chat.repository.ChatRoomMemberRepository;
import com.example.chatservice.chat.repository.ChatRoomRepository;
import com.example.chatservice.common.exception.CustomException;
import com.example.chatservice.kafka.producer.KafkaProducerService;
import com.example.chatservice.member.client.MemberFeignClient;
import com.example.chatservice.member.dto.MemberDto;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.modelmapper.ModelMapper;

/**
 * 트랜잭션 프록시가 없는 순수 단위 테스트다.
 * TransactionSynchronizationManager.isSynchronizationActive() 가 false 이므로
 * ChatRoomService 는 커밋 콜백을 등록하지 못하고 즉시 발행한다 — 그 경로를 검증한다.
 */
class ChatRoomServiceTest {

    private ChatRoomMemberRepository chatRoomMemberRepository;
    private ChatRoomRepository chatRoomRepository;
    private ChatRepository chatRepository;
    private MemberFeignClient memberFeignClient;
    private KafkaProducerService kafkaProducerService;
    private ChatRoomService chatRoomService;

    @BeforeEach
    void setUp() {
        chatRoomMemberRepository = mock(ChatRoomMemberRepository.class);
        chatRoomRepository = mock(ChatRoomRepository.class);
        chatRepository = mock(ChatRepository.class);
        memberFeignClient = mock(MemberFeignClient.class);
        kafkaProducerService = mock(KafkaProducerService.class);

        chatRoomService = new ChatRoomService(
                chatRoomMemberRepository,
                chatRoomRepository,
                chatRepository,
                memberFeignClient,
                new ModelMapper(),
                kafkaProducerService);

        // save 는 넘겨받은 엔티티를 그대로 돌려준다 - JPA 저장 흉내.
        when(chatRoomRepository.save(any(ChatRoom.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private MemberDto member(Long id) {
        return MemberDto.builder().id(id).userId("user-" + id).build();
    }

    @Test
    @DisplayName("방을 만들면 topic-chat-room-created 로 roomId 를 발행한다")
    void createChatRoom_publishesRoomCreatedEvent() {
        when(memberFeignClient.getMembersById(anyList()))
                .thenReturn(List.of(member(1L), member(2L)));

        var chatRoomDto = chatRoomService.createChatRoom(List.of(1L, 2L));

        verify(kafkaProducerService).sendRoomCreatedMessage(chatRoomDto.getRoomId());
        assertThat(chatRoomDto.getRoomId()).isNotBlank();
    }

    @Test
    @DisplayName("멤버를 하나도 못 찾으면 방을 만들지 않고 발행도 하지 않는다")
    void createChatRoom_withNoMembers_doesNotPublish() {
        when(memberFeignClient.getMembersById(anyList())).thenReturn(List.of());

        try {
            chatRoomService.createChatRoom(List.of(999L));
        } catch (CustomException ignored) {
            // 멤버 없음은 예외로 처리된다 - 여기서 검증하려는 것은 발행 여부다.
        }

        verify(kafkaProducerService, never()).sendRoomCreatedMessage(anyString());
    }

    /** 이미 저장된 방 하나를 흉내 낸다. 넘긴 memberId 마다 ChatRoomMember 를 붙인다. */
    private ChatRoom existingRoom(String roomId, Long... memberIds) {
        ChatRoom room = new ChatRoom(roomId, "새로운 채팅방", "", "", "", "2026-09-08 00:00:00");
        for (Long memberId : memberIds) {
            room.getChatRoomMemberList().add(new ChatRoomMember(memberId, "", room));
        }
        return room;
    }

    @Test
    @DisplayName("같은 멤버 구성의 방이 있으면 새로 만들지 않고 그 방을 돌려준다")
    void createChatRoom_withSameMemberSet_returnsExistingRoom() {
        ChatRoom existing = existingRoom("room-existing", 1L, 2L);
        when(memberFeignClient.getMembersById(anyList()))
                .thenReturn(List.of(member(1L), member(2L)));
        when(chatRoomMemberRepository.findRoomIdByExactMemberIds(Set.of(1L, 2L)))
                .thenReturn(Optional.of(10L));
        when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(existing));

        var chatRoomDto = chatRoomService.createChatRoom(List.of(2L, 1L));

        assertThat(chatRoomDto.getRoomId()).isEqualTo("room-existing");
        assertThat(chatRoomDto.getMembers()).extracting("id").containsExactlyInAnyOrder(1L, 2L);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        verify(kafkaProducerService, never()).sendRoomCreatedMessage(anyString());
    }

    @Test
    @DisplayName("같은 멤버 구성의 방이 없으면 새 방을 만들고 발행한다")
    void createChatRoom_withoutMatchingRoom_createsNewRoom() {
        when(memberFeignClient.getMembersById(anyList()))
                .thenReturn(List.of(member(1L), member(2L)));
        when(chatRoomMemberRepository.findRoomIdByExactMemberIds(anySet()))
                .thenReturn(Optional.empty());

        var chatRoomDto = chatRoomService.createChatRoom(List.of(1L, 2L));

        verify(chatRoomMemberRepository).findRoomIdByExactMemberIds(Set.of(1L, 2L));
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(kafkaProducerService).sendRoomCreatedMessage(chatRoomDto.getRoomId());
    }

    @Test
    @DisplayName("요청 id 에 중복이 있어도 멤버는 한 번만 등록한다")
    void createChatRoom_withDuplicateIds_registersEachMemberOnce() {
        when(memberFeignClient.getMembersById(List.of(1L, 2L)))
                .thenReturn(List.of(member(1L), member(2L)));

        chatRoomService.createChatRoom(List.of(1L, 1L, 2L));

        ArgumentCaptor<ChatRoom> saved = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRoomRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getChatRoomMemberList())
                .extracting(ChatRoomMember::getMemberId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("속한 방이 없으면 member-service 를 호출하지 않고 빈 목록을 돌려준다")
    void searchChatRoomByUserId_withNoRooms_returnsEmptyWithoutMemberLookup() {
        when(chatRoomMemberRepository.findAllByMemberId(48L)).thenReturn(List.of());

        var rooms = chatRoomService.searchChatRoomByUserId("48");

        assertThat(rooms).isEmpty();
        verify(memberFeignClient, never()).getMembersById(anyList());
    }
}
