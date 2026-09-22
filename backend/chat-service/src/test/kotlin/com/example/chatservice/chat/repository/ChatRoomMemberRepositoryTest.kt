package com.example.chatservice.chat.repository

import com.example.chatservice.chat.entity.ChatRoom
import com.example.chatservice.chat.entity.ChatRoomMember
import com.example.chatservice.config.QuerydslConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import

/** H2 에서 QueryDSL 집계 쿼리가 멤버 집합이 정확히 같은 방만 찾는지 검증한다. */
@DataJpaTest
@Import(QuerydslConfig::class)
class ChatRoomMemberRepositoryTest {

    @Autowired lateinit var chatRoomRepository: ChatRoomRepository
    @Autowired lateinit var chatRoomMemberRepository: ChatRoomMemberRepository

    private var pairRoomId: Long? = null
    private var groupRoomId: Long? = null

    @BeforeEach
    fun setUp() {
        pairRoomId = saveRoom("room-pair", 1L, 2L)
        groupRoomId = saveRoom("room-group", 1L, 2L, 3L)
        saveRoom("room-other", 2L, 3L)
    }

    private fun saveRoom(roomId: String, vararg memberIds: Long): Long? {
        val room = chatRoomRepository.save(ChatRoom(roomId, "새로운 채팅방", "", "", "", "2026-09-08 00:00:00"))
        for (memberId in memberIds) {
            chatRoomMemberRepository.save(ChatRoomMember(memberId, "", room))
        }
        return room.id
    }

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
