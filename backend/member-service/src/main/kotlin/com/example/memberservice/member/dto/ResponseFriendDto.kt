package com.example.memberservice.member.dto

import com.example.memberservice.member.entity.FriendStatus
import com.example.memberservice.member.entity.MemberFriend
import com.example.memberservice.member.entity.Role

class ResponseFriendDto(
    var id: Long? = null,
    var userId: String? = null,
    var auth: String? = null,
    var role: Role? = null,
    var email: String? = null,
    var username: String? = null,
    var statusMessage: String? = null,
    var profileImage: String? = null,
    var wallpaperImage: String? = null,
    /** 내가 정한 친구 이름. 비어 있으면 클라이언트는 username 을 쓴다. */
    var friendName: String? = null,
    /** 내가 이 친구를 즐겨찾기했는지. */
    var favorite: Boolean = false,
    status: FriendStatus? = null,
) {
    /** 내가 이 친구에게 매긴 상태(NORMAL/HIDDEN/BLOCKED). 친구 행 없이 만든 DTO(회원 검색 결과 등)도 null 대신 NORMAL 로 내려간다. */
    var status: FriendStatus? = status
        get() = field ?: FriendStatus.NORMAL

    constructor(memberDto: MemberDto) : this(
        id = memberDto.id,
        userId = memberDto.userId,
        auth = memberDto.auth,
        role = memberDto.role,
        email = memberDto.email,
        username = memberDto.username,
        statusMessage = memberDto.statusMessage,
        profileImage = memberDto.profileImage,
        wallpaperImage = memberDto.wallpaperImage,
    )

    companion object {
        @JvmStatic
        fun from(memberFriend: MemberFriend): ResponseFriendDto {
            val dto = ResponseFriendDto(MemberDto(memberFriend.friend))
            dto.friendName = memberFriend.friendName
            dto.favorite = memberFriend.favorite
            dto.status = memberFriend.status
            return dto
        }
    }
}
