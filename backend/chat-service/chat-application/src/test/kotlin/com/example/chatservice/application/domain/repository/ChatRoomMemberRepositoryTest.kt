package com.example.chatservice.application.domain.repository

import com.example.chatservice.application.domain.repository.rw.ChatRoomMemberRwRepository
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

/** H2 에서 QueryDSL 집계 쿼리가 멤버 집합이 정확히 같은 방만 찾는지 검증한다. 방 만들기 전의 검사라 master 저장소다. */
@SpringBootTest
class ChatRoomMemberRepositoryTest {

    @Autowired lateinit var chatRoomMemberRepository: ChatRoomMemberRwRepository
    @Autowired lateinit var fixtures: ChatFixtures
    @MockitoBean lateinit var memberGateway: MemberGateway
    @MockitoBean lateinit var chatEventPublisher: ChatEventPublisher

    private var pairRoomId: Long? = null
    private var groupRoomId: Long? = null

    @BeforeEach
    fun setUp() {
        fixtures.clear()
        pairRoomId = fixtures.room("room-pair", 1L, 2L).id
        groupRoomId = fixtures.room("room-group", 1L, 2L, 3L).id
        fixtures.room("room-other", 2L, 3L)
    }

    @AfterEach
    fun cleanUp() = fixtures.clear()

    @Test
    @DisplayName("멤버 집합이 정확히 같은 방을 순서와 무관하게 찾는다")
    fun findsRoomWithExactMemberSet() {
        assertThat(chatRoomMemberRepository.findRoomIdByExactMemberIds(setOf(2L, 1L))).contains(pairRoomId)
        assertThat(chatRoomMemberRepository.findRoomIdByExactMemberIds(setOf(3L, 1L, 2L))).contains(groupRoomId)
    }

    @Test
    @DisplayName("일부만 겹치는 방은 찾지 않는다")
    fun ignoresPartiallyOverlappingRooms() {
        assertThat(chatRoomMemberRepository.findRoomIdByExactMemberIds(setOf(1L, 3L))).isEmpty
        assertThat(chatRoomMemberRepository.findRoomIdByExactMemberIds(setOf(1L))).isEmpty
        assertThat(chatRoomMemberRepository.findRoomIdByExactMemberIds(setOf(1L, 2L, 3L, 4L))).isEmpty
    }

    @Test
    @DisplayName("빈 집합이면 아무 방도 찾지 않는다")
    fun emptySetFindsNothing() {
        assertThat(chatRoomMemberRepository.findRoomIdByExactMemberIds(setOf())).isEmpty
    }
}
