package com.example.chatservice.application.access

/**
 * 요청한 사람. 게이트웨이가 넣어 준 X-Auth-User-Id([userId], 모두 계정 subject)와
 * 그 회원의 숫자 PK([memberId]). 방 멤버 행(chat_room_member)은 memberId 로, 메시지의 sender 는 userId 로 적혀 있다.
 */
data class Caller(val userId: String, val memberId: Long) {

    /** 경로·본문의 값이 나를 가리키는지. 앱에 따라 userId 를 넣기도, 회원 id(숫자)를 넣기도 한다. */
    fun isSelf(id: String?): Boolean = id != null && (id == userId || id == memberId.toString())
}

/** 방의 멤버 구성. 없는 방이면 [exists] 가 false 이고 멤버는 비어 있다. */
data class RoomMembership(val exists: Boolean, val memberIds: List<Long>) {

    /** 1:1 방의 정의: 방 멤버가 정확히 2명. */
    val oneOnOne: Boolean get() = memberIds.size == ONE_ON_ONE_MEMBER_COUNT

    fun has(memberId: Long): Boolean = memberIds.contains(memberId)

    companion object {
        const val ONE_ON_ONE_MEMBER_COUNT = 2
        val NONE = RoomMembership(false, emptyList())
    }
}
