package com.example.chatservice.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chatservice.chat.entity.Chat;
import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.ChatRoomMember;
import com.example.chatservice.chat.entity.ChatType;
import com.example.chatservice.config.QuerydslConfig;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 차단 제외 조건을 H2 에서 확인한다.
 * 핵심은 "limit 앞에서 걸러진다"는 것이다 — 메모리에서 지우면 3개 달라고 했는데 1개만 오는
 * 페이지가 생기고, 앱은 그걸 끝으로 알아 더 못 불러온다.
 */
@DataJpaTest
@Import(QuerydslConfig.class)
class ChatBlockedSenderQueryTest {

    private static final String ROOM = "room-1x1";
    private static final String GROUP_ROOM = "room-group";
    private static final Set<String> BLOCKED = Set.of("bad");

    @Autowired ChatRepository chatRepository;
    @Autowired ChatRoomRepository chatRoomRepository;
    @Autowired ChatRoomMemberRepository chatRoomMemberRepository;

    private ChatRoom oneOnOne;
    private ChatRoom group;

    @BeforeEach
    void setUp() {
        oneOnOne = saveRoom(ROOM, 1L, 2L);
        group = saveRoom(GROUP_ROOM, 1L, 2L, 3L);

        // 1:1 방: 차단한 bad 와 정상 good 이 번갈아 보낸다.
        saveChat(oneOnOne, "bad", "b1", ChatType.CHAT_TYPE_TEXT);   // id 1
        saveChat(oneOnOne, "good", "g1", ChatType.CHAT_TYPE_TEXT);  // id 2
        saveChat(oneOnOne, "bad", "b2", ChatType.CHAT_TYPE_IMAGE);  // id 3
        saveChat(oneOnOne, "good", "g2", ChatType.CHAT_TYPE_IMAGE); // id 4
        saveChat(oneOnOne, "bad", "b3", ChatType.CHAT_TYPE_TEXT);   // id 5
        saveChat(oneOnOne, "good", "g3", ChatType.CHAT_TYPE_TEXT);  // id 6

        // 단체방: 같은 사람이 보낸 메시지도 남아야 한다.
        saveChat(group, "bad", "group-bad", ChatType.CHAT_TYPE_TEXT); // id 7
    }

    private ChatRoom saveRoom(String roomId, Long... memberIds) {
        ChatRoom room = chatRoomRepository.save(new ChatRoom(roomId, roomId, "", "", "", "2026-09-13 00:00:00"));
        for (Long memberId : memberIds) {
            chatRoomMemberRepository.save(new ChatRoomMember(memberId, "", room));
        }
        return room;
    }

    private Chat saveChat(ChatRoom room, String sender, String message, int type) {
        return chatRepository.save(new Chat(message, room.getRoomId(), room, sender, "2026-09-13 00:00:00", type));
    }

    @Test
    @DisplayName("방 전체 이력에서 차단한 사람의 메시지가 빠진다")
    void findAllByRoomIdExcludesBlocked() {
        assertThat(chatRepository.findAllByRoomId(ROOM, BLOCKED))
                .extracting(Chat::getMessage).containsExactlyInAnyOrder("g1", "g2", "g3");
        assertThat(chatRepository.findAllByRoomId(ROOM, Set.of()))
                .hasSize(6);
    }

    @Test
    @DisplayName("size 페이지는 거른 뒤에도 size 만큼 채워진다")
    void sizePageStaysFullAfterExcluding() {
        List<Chat> page = chatRepository.findByRoomIdSize(ROOM, 3L, BLOCKED);

        assertThat(page).hasSize(3);
        assertThat(page).extracting(Chat::getSender).containsOnly("good");
    }

    @Test
    @DisplayName("이전 메시지 페이지도 거른 뒤 size 를 채운다")
    void prevPageStaysFullAfterExcluding() {
        List<Chat> page = chatRepository.findByRoomIdAndChatId(ROOM, 100L, 2L, BLOCKED);

        assertThat(page).hasSize(2);
        assertThat(page).extracting(Chat::getSender).containsOnly("good");
    }

    @Test
    @DisplayName("이미지 목록도 차단한 사람의 것을 뺀다")
    void imagesExcludeBlocked() {
        assertThat(chatRepository.findByImageChatSize(ROOM, 10L, BLOCKED))
                .extracting(Chat::getMessage).containsExactly("g2");
        assertThat(chatRepository.findByImageChatSize(ROOM, 10L, Set.of()))
                .hasSize(2);
    }

    @Test
    @DisplayName("id 목록 조회는 1:1 방만 거르고 단체방 메시지는 남긴다")
    void idLookupFiltersOnlyOneOnOneRooms() {
        List<Long> allIds = chatRepository.findAll().stream().map(Chat::getId).toList();

        assertThat(chatRepository.findAllByIdIn(allIds, BLOCKED))
                .extracting(Chat::getMessage)
                .containsExactlyInAnyOrder("g1", "g2", "g3", "group-bad");

        assertThat(chatRepository.findAllByIdIn(allIds, Set.of())).hasSize(7);
        assertThat(chatRepository.findAllByIdIn(List.of(), BLOCKED)).isEmpty();
    }

    @Test
    @DisplayName("미읽음 개수에서 차단한 사람의 메시지가 빠진다")
    void unreadCountExcludesBlocked() {
        Long lastId = chatRepository.findAllByRoomId(ROOM, Set.of()).stream()
                .map(Chat::getId).max(Long::compareTo).orElseThrow();

        assertThat(chatRepository.countByRoomIdAndIdBetween(ROOM, 1L, lastId, BLOCKED)).isEqualTo(3L);
        assertThat(chatRepository.countByRoomIdAndIdBetween(ROOM, 1L, lastId, Set.of())).isEqualTo(6L);
    }

    @Test
    @DisplayName("sender 가 없는 옛날 메시지는 NOT IN 에 걸려 사라지지 않는다")
    void nullSenderSurvivesExclusion() {
        chatRepository.save(new Chat("legacy", ROOM, oneOnOne, null, "2026-09-13 00:00:00", ChatType.CHAT_TYPE_TEXT));

        assertThat(chatRepository.findAllByRoomId(ROOM, BLOCKED))
                .extracting(Chat::getMessage).contains("legacy");
    }
}
