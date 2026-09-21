package com.example.chatservice.chat.repository

import com.example.chatservice.chat.entity.Chat
import com.example.chatservice.chat.entity.ChatRoom
import com.example.chatservice.chat.entity.ChatRoomMember
import com.example.chatservice.chat.entity.ChatType
import com.example.chatservice.config.QuerydslConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import

/**
 * 차단 제외 조건을 H2 에서 확인한다.
 * 핵심은 "limit 앞에서 걸러진다"는 것이다 — 메모리에서 지우면 3개 달라고 했는데 1개만 오는
 * 페이지가 생기고, 앱은 그걸 끝으로 알아 더 못 불러온다.
 */
@DataJpaTest
@Import(QuerydslConfig::class)
class ChatBlockedSenderQueryTest {

    companion object {
        private const val ROOM = "room-1x1"
        private const val GROUP_ROOM = "room-group"
        private val BLOCKED = setOf("bad")
    }

    @Autowired lateinit var chatRepository: ChatRepository
    @Autowired lateinit var chatRoomRepository: ChatRoomRepository
    @Autowired lateinit var chatRoomMemberRepository: ChatRoomMemberRepository

    private lateinit var oneOnOne: ChatRoom
    private lateinit var group: ChatRoom

    @BeforeEach
    fun setUp() {
        oneOnOne = saveRoom(ROOM, 1L, 2L)
        group = saveRoom(GROUP_ROOM, 1L, 2L, 3L)

        // 1:1 방: 차단한 bad 와 정상 good 이 번갈아 보낸다.
        saveChat(oneOnOne, "bad", "b1", ChatType.CHAT_TYPE_TEXT) // id 1
        saveChat(oneOnOne, "good", "g1", ChatType.CHAT_TYPE_TEXT) // id 2
        saveChat(oneOnOne, "bad", "b2", ChatType.CHAT_TYPE_IMAGE) // id 3
        saveChat(oneOnOne, "good", "g2", ChatType.CHAT_TYPE_IMAGE) // id 4
        saveChat(oneOnOne, "bad", "b3", ChatType.CHAT_TYPE_TEXT) // id 5
        saveChat(oneOnOne, "good", "g3", ChatType.CHAT_TYPE_TEXT) // id 6

        // 단체방: 같은 사람이 보낸 메시지도 남아야 한다.
        saveChat(group, "bad", "group-bad", ChatType.CHAT_TYPE_TEXT) // id 7
    }

    private fun saveRoom(roomId: String, vararg memberIds: Long): ChatRoom {
        val room = chatRoomRepository.save(ChatRoom(roomId, roomId, "", "", "", "2026-09-13 00:00:00"))
        for (memberId in memberIds) {
            chatRoomMemberRepository.save(ChatRoomMember(memberId, "", room))
        }
        return room
    }

    private fun saveChat(room: ChatRoom, sender: String?, message: String, type: Int): Chat =
        chatRepository.save(Chat(message, room.roomId, room, sender, "2026-09-13 00:00:00", type))

    @Test
    @DisplayName("방 전체 이력에서 차단한 사람의 메시지가 빠진다")
    fun findAllByRoomIdExcludesBlocked() {
        assertThat(chatRepository.findAllByRoomId(ROOM, BLOCKED))
            .extracting<String> { it.message }.containsExactlyInAnyOrder("g1", "g2", "g3")
        assertThat(chatRepository.findAllByRoomId(ROOM, emptySet()))
            .hasSize(6)
    }

    @Test
    @DisplayName("size 페이지는 거른 뒤에도 size 만큼 채워진다")
    fun sizePageStaysFullAfterExcluding() {
        val page = chatRepository.findByRoomIdSize(ROOM, 3L, BLOCKED)

        assertThat(page).hasSize(3)
        assertThat(page).extracting<String> { it.sender }.containsOnly("good")
    }

    @Test
    @DisplayName("이전 메시지 페이지도 거른 뒤 size 를 채운다")
    fun prevPageStaysFullAfterExcluding() {
        val page = chatRepository.findByRoomIdAndChatId(ROOM, 100L, 2L, BLOCKED)

        assertThat(page).hasSize(2)
        assertThat(page).extracting<String> { it.sender }.containsOnly("good")
    }

    @Test
    @DisplayName("이미지 목록도 차단한 사람의 것을 뺀다")
    fun imagesExcludeBlocked() {
        assertThat(chatRepository.findByImageChatSize(ROOM, 10L, BLOCKED))
            .extracting<String> { it.message }.containsExactly("g2")
        assertThat(chatRepository.findByImageChatSize(ROOM, 10L, emptySet()))
            .hasSize(2)
    }

    @Test
    @DisplayName("id 목록 조회는 1:1 방만 거르고 단체방 메시지는 남긴다")
    fun idLookupFiltersOnlyOneOnOneRooms() {
        val allIds = chatRepository.findAll().map { it.id!! }

        assertThat(chatRepository.findAllByIdIn(allIds, BLOCKED))
            .extracting<String> { it.message }
            .containsExactlyInAnyOrder("g1", "g2", "g3", "group-bad")

        assertThat(chatRepository.findAllByIdIn(allIds, emptySet())).hasSize(7)
        assertThat(chatRepository.findAllByIdIn(listOf(), BLOCKED)).isEmpty()
    }

    @Test
    @DisplayName("미읽음 개수에서 차단한 사람의 메시지가 빠진다")
    fun unreadCountExcludesBlocked() {
        val lastId = chatRepository.findAllByRoomId(ROOM, emptySet()).maxOf { it.id!! }

        assertThat(chatRepository.countByRoomIdAndIdBetween(ROOM, 1L, lastId, BLOCKED)).isEqualTo(3L)
        assertThat(chatRepository.countByRoomIdAndIdBetween(ROOM, 1L, lastId, emptySet())).isEqualTo(6L)
    }

    @Test
    @DisplayName("sender 가 없는 옛날 메시지는 NOT IN 에 걸려 사라지지 않는다")
    fun nullSenderSurvivesExclusion() {
        chatRepository.save(Chat("legacy", ROOM, oneOnOne, null, "2026-09-13 00:00:00", ChatType.CHAT_TYPE_TEXT))

        assertThat(chatRepository.findAllByRoomId(ROOM, BLOCKED))
            .extracting<String> { it.message }.contains("legacy")
    }
}
