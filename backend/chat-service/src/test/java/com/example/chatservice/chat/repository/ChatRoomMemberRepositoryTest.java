package com.example.chatservice.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.ChatRoomMember;
import com.example.chatservice.config.QuerydslConfig;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/** H2 에서 QueryDSL 집계 쿼리가 멤버 집합이 정확히 같은 방만 찾는지 검증한다. */
@DataJpaTest
@Import(QuerydslConfig.class)
class ChatRoomMemberRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    private Long pairRoomId;
    private Long groupRoomId;

    @BeforeEach
    void setUp() {
        pairRoomId = saveRoom("room-pair", 1L, 2L);
        groupRoomId = saveRoom("room-group", 1L, 2L, 3L);
        saveRoom("room-other", 2L, 3L);
    }

    private Long saveRoom(String roomId, Long... memberIds) {
        ChatRoom room = chatRoomRepository.save(new ChatRoom(roomId, "새로운 채팅방", "", "", "", "2026-09-08 00:00:00"));
        for (Long memberId : memberIds) {
            chatRoomMemberRepository.save(new ChatRoomMember(memberId, "", room));
        }
        return room.getId();
    }

    @Test
    @DisplayName("멤버 집합이 정확히 같은 방을 순서와 무관하게 찾는다")
    void findsRoomWithExactMemberSet() {
        assertThat(chatRoomMemberRepository.findRoomIdByExactMemberIds(Set.of(2L, 1L))).contains(pairRoomId);
        assertThat(chatRoomMemberRepository.findRoomIdByExactMemberIds(Set.of(3L, 1L, 2L))).contains(groupRoomId);
    }

    @Test
    @DisplayName("일부만 겹치는 방은 찾지 않는다")
    void ignoresPartiallyOverlappingRooms() {
        assertThat(chatRoomMemberRepository.findRoomIdByExactMemberIds(Set.of(1L, 3L))).isEmpty();
        assertThat(chatRoomMemberRepository.findRoomIdByExactMemberIds(Set.of(1L))).isEmpty();
        assertThat(chatRoomMemberRepository.findRoomIdByExactMemberIds(Set.of(1L, 2L, 3L, 4L))).isEmpty();
    }

    @Test
    @DisplayName("빈 집합이면 아무 방도 찾지 않는다")
    void emptySetFindsNothing() {
        assertThat(chatRoomMemberRepository.findRoomIdByExactMemberIds(Set.of())).isEmpty();
    }
}
