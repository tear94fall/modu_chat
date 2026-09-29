package com.example.memberservice.application.domain.repository.query

/** 한 회원이 소유한 친구 행(member_friend.member_id = 회원)의 상태별 수. 필터와 같은 기준으로 센다. */
data class FriendCounts(
    val all: Long = 0,
    val normal: Long = 0,
    val favorite: Long = 0,
    val hidden: Long = 0,
    val blocked: Long = 0,
) {
    fun of(filter: AdminFriendFilter): Long = when (filter) {
        AdminFriendFilter.ALL -> all
        AdminFriendFilter.NORMAL -> normal
        AdminFriendFilter.FAVORITE -> favorite
        AdminFriendFilter.HIDDEN -> hidden
        AdminFriendFilter.BLOCKED -> blocked
    }
}
