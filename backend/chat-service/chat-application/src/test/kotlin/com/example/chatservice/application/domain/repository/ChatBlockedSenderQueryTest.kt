package com.example.chatservice.application.domain.repository

import com.example.chatservice.application.domain.entity.ChatRoom
import com.example.chatservice.application.domain.entity.ChatType
import com.example.chatservice.application.domain.repository.ro.ChatRoRepository
import com.example.chatservice.application.domain.repository.rw.ChatRwRepository
import com.example.chatservice.application.event.ChatEventPublisher
import com.example.chatservice.application.member.MemberGateway
import com.example.chatservice.application.support.ChatFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * 차단 제외 조건을 H2 에서 확인한다. 둘러보기 질의는 replica 저장소로, 쓰기 직후에 읽히는 질의는 master 저장소로 본다
 * (질의 자체는 ChatQueries 하나다).
 * 핵심은 "limit 앞에서 걸러진다"는 것이다 — 메모리에서 지우면 3개 달라고 했는데 1개만 오는
 * 페이지가 생기고, 앱은 그걸 끝으로 알아 더 못 불러온다.
 */
@SpringBootTest
class ChatBlockedSenderQueryTest {

    companion object {
        private const val ROOM = "room-1x1"
        private const val GROUP_ROOM = "room-group"
        private val BLOCKED = setOf("bad")
    }

    @Autowired lateinit var chatRoRepository: ChatRoRepository
    @Autowired lateinit var chatRwRepository: ChatRwRepository
    @Autowired lateinit var fixtures: ChatFixtures
    @MockitoBean lateinit var memberGateway: MemberGateway
    @MockitoBean lateinit var chatEventPublisher: ChatEventPublisher

    private lateinit var oneOnOne: ChatRoom
    private lateinit var group: ChatRoom

    @BeforeEach
    fun setUp() {
        fixtures.clear()
        oneOnOne = fixtures.room(ROOM, 1L, 2L)
        group = fixtures.room(GROUP_ROOM, 1L, 2L, 3L)

        // 1:1 방: 차단한 bad 와 정상 good 이 번갈아 보낸다.
        fixtures.chat(oneOnOne, "bad", "b1", ChatType.CHAT_TYPE_TEXT)
        fixtures.chat(oneOnOne, "good", "g1", ChatType.CHAT_TYPE_TEXT)
        fixtures.chat(oneOnOne, "bad", "b2", ChatType.CHAT_TYPE_IMAGE)
        fixtures.chat(oneOnOne, "good", "g2", ChatType.CHAT_TYPE_IMAGE)
        fixtures.chat(oneOnOne, "bad", "b3", ChatType.CHAT_TYPE_TEXT)
        fixtures.chat(oneOnOne, "good", "g3", ChatType.CHAT_TYPE_TEXT)

        // 단체방: 같은 사람이 보낸 메시지도 남아야 한다.
        fixtures.chat(group, "bad", "group-bad", ChatType.CHAT_TYPE_TEXT)
    }

    @AfterEach
    fun cleanUp() = fixtures.clear()

    @Test
    @DisplayName("방 전체 이력에서 차단한 사람의 메시지가 빠진다")
    fun findAllByRoomIdExcludesBlocked() {
        assertThat(chatRoRepository.findAllByRoomId(ROOM, BLOCKED))
            .extracting<String> { it.message }.containsExactlyInAnyOrder("g1", "g2", "g3")
        assertThat(chatRoRepository.findAllByRoomId(ROOM, emptySet()))
            .hasSize(6)
    }

    @Test
    @DisplayName("size 페이지는 거른 뒤에도 size 만큼 채워진다")
    fun sizePageStaysFullAfterExcluding() {
        for (page in listOf(chatRwRepository.findByRoomIdSize(ROOM, 3L, BLOCKED), chatRoRepository.findByRoomIdSize(ROOM, 3L, BLOCKED))) {
            assertThat(page).hasSize(3)
            assertThat(page).extracting<String> { it.sender }.containsOnly("good")
        }
    }

    @Test
    @DisplayName("이전 메시지 페이지도 거른 뒤 size 를 채운다")
    fun prevPageStaysFullAfterExcluding() {
        val page = chatRoRepository.findByRoomIdAndChatId(ROOM, Long.MAX_VALUE, 2L, BLOCKED)

        assertThat(page).hasSize(2)
        assertThat(page).extracting<String> { it.sender }.containsOnly("good")
    }

    @Test
    @DisplayName("이미지 목록도 차단한 사람의 것을 뺀다")
    fun imagesExcludeBlocked() {
        assertThat(chatRoRepository.findByImageChatSize(ROOM, 10L, BLOCKED))
            .extracting<String> { it.message }.containsExactly("g2")
        assertThat(chatRoRepository.findByImageChatSize(ROOM, 10L, emptySet()))
            .hasSize(2)
    }

    @Test
    @DisplayName("id 목록 조회는 1:1 방만 거르고 단체방 메시지는 남긴다")
    fun idLookupFiltersOnlyOneOnOneRooms() {
        val allIds = chatRwRepository.findAll().map { it.id!! }

        assertThat(chatRwRepository.findAllByIdIn(allIds, BLOCKED))
            .extracting<String> { it.message }
            .containsExactlyInAnyOrder("g1", "g2", "g3", "group-bad")

        assertThat(chatRwRepository.findAllByIdIn(allIds, emptySet())).hasSize(7)
        assertThat(chatRwRepository.findAllByIdIn(listOf(), BLOCKED)).isEmpty()
    }

    @Test
    @DisplayName("미읽음 개수에서 차단한 사람의 메시지가 빠진다")
    fun unreadCountExcludesBlocked() {
        val ids = chatRoRepository.findAllByRoomId(ROOM, emptySet()).map { it.id!! }

        assertThat(chatRwRepository.countByRoomIdAndIdBetween(ROOM, ids.min(), ids.max(), BLOCKED)).isEqualTo(3L)
        assertThat(chatRwRepository.countByRoomIdAndIdBetween(ROOM, ids.min(), ids.max(), emptySet())).isEqualTo(6L)
    }

    @Test
    @DisplayName("sender 가 없는 옛날 메시지는 NOT IN 에 걸려 사라지지 않는다")
    fun nullSenderSurvivesExclusion() {
        fixtures.chat(oneOnOne, null, "legacy", ChatType.CHAT_TYPE_TEXT)

        assertThat(chatRoRepository.findAllByRoomId(ROOM, BLOCKED))
            .extracting<String> { it.message }.contains("legacy")
    }
}
