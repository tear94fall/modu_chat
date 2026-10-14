package com.example.chatservice.application.service

import com.example.chatservice.application.domain.entity.ChatRoom
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/** 방 만들기의 멤버 구성 값: 순서·중복에 상관없이 같고, member_key 는 16진수 소문자 64자다. */
class RoomMemberSetTest {

    @Test
    fun key_isSortedIdsJoinedWithComma_regardlessOfOrderAndDuplicates() {
        assertThat(RoomMemberSet(listOf(12L, 3L, 7L, 3L)).key).isEqualTo("3,7,12")
        assertThat(RoomMemberSet(setOf(2L, 1L)).key).isEqualTo(RoomMemberSet(listOf(1L, 2L)).key)
        assertThat(RoomMemberSet(listOf(5L)).key).isEqualTo("5")
    }

    @Test
    fun ids_areSorted() {
        assertThat(RoomMemberSet(listOf(9L, 1L, 4L)).ids).containsExactly(1L, 4L, 9L)
    }

    @Test
    fun memberKey_is64LowercaseHexChars_andFitsTheColumn() {
        val memberKey = RoomMemberSet(listOf(78L, 79L)).memberKey

        assertThat(memberKey).hasSize(ChatRoom.MEMBER_KEY_LENGTH)
        assertThat(memberKey).matches("[0-9a-f]{64}")
    }

    @Test
    fun memberKey_isDeterministic_andOrderIndependent() {
        val a = RoomMemberSet(listOf(2L, 1L))
        val b = RoomMemberSet(listOf(1L, 2L, 2L))

        assertThat(a.memberKey).isEqualTo(b.memberKey)
        assertThat(a.memberKey).isEqualTo(RoomMemberSet(listOf(1L, 2L)).memberKey)
        // SHA-256("1,2") 를 못 박는다. 알고리즘이 바뀌면(배포 중 두 버전이 다른 키를 쓰게 된다) 여기서 걸린다.
        assertThat(a.memberKey).isEqualTo("17f8af97ad4a7f7639a4c9171d5185cbafb85462877a4746c21bdb0a4f940ca0")
        assertThat(RoomMemberSet(listOf(78L, 79L)).memberKey)
            .isEqualTo("ce294127237ae576b5b0681fdd4fffdb75253f4238a9855bbf973b34c6e01c2d")
    }

    @Test
    fun memberKey_differsForDifferentMemberSets() {
        val keys = listOf(
            RoomMemberSet(listOf(1L, 2L)),
            RoomMemberSet(listOf(1L, 2L, 3L)),
            RoomMemberSet(listOf(1L, 12L)),
            RoomMemberSet(listOf(12L)),
        ).map { it.memberKey }

        assertThat(keys).doesNotHaveDuplicates()
    }

    @Test
    fun empty_isRejected() {
        assertThatThrownBy { RoomMemberSet(emptyList()) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
