package com.example.modumessenger.core.model

/** 로그인한 사람과 친구를 같은 모양으로 다룬다. 서버의 `ResponseMemberDto`/`ResponseFriendDto` 를 합친 모습. */
data class Member(
    val id: Long = 0L,
    val userId: String = "",
    val email: String = "",
    val username: String = "",
    val statusMessage: String = "",
    val profileImage: String = "",
    val wallpaperImage: String = "",
    val role: Role = Role.USER,
    val profiles: List<Profile> = emptyList(),
    /** 친구 목록 응답에만 들어 있는 "내가 이 친구에게 붙인 이름". */
    val friendName: String? = null,
    /** 즐겨찾기한 친구인가(친구 응답에만 들어온다). */
    val favorite: Boolean = false,
    /** 친구 상태(친구 응답에만 들어온다). 친구가 아니면 [FriendStatus.NORMAL] 이다. */
    val friendStatus: FriendStatus = FriendStatus.NORMAL,
)

/** 서버 JSON 은 `"ROLE_ADMIN"`/`"ROLE_USER"` 이고, 매핑은 [com.example.modumessenger.data.dto.roleOf] 가 한다. */
enum class Role { ADMIN, USER }
