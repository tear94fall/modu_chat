package com.example.chatservice.application.service

import com.example.chatservice.application.common.lock.Lockable
import java.security.MessageDigest

/**
 * 방 만들기의 멤버 구성(회원 id 집합). 같은 구성이면 넣은 순서와 상관없이 같은 값이 된다.
 *
 * - [key]: 정렬·중복 제거한 id 를 ',' 로 이은 값(예: `78,79`). Redis 락(@ApiLock) 키가 된다.
 * - [memberKey]: [key] 의 SHA-256 16진수 소문자 64자. `chat_room.member_key` 에 들어가고,
 *   그 열의 유니크 키(uk_chat_room_member_key)가 같은 구성의 방이 동시에 두 개 생기는 것을 막는다.
 *   길이가 일정해(CHAR(64)) 멤버가 몇 명이든 열 하나로 끝난다.
 */
class RoomMemberSet(memberIds: Collection<Long>) : Lockable {

    val ids: Set<Long> = memberIds.toSortedSet()

    init {
        require(ids.isNotEmpty()) { "방 멤버가 없습니다" }
    }

    override val key: String = ids.joinToString(",")

    val memberKey: String = sha256Hex(key)

    override fun toString(): String = "RoomMemberSet($key)"

    companion object {
        const val LOCK_PREFIX = "chat:room-create"

        fun sha256Hex(value: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
