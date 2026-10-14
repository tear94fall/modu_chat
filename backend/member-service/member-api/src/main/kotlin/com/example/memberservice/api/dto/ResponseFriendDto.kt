package com.example.memberservice.api.dto

import com.example.memberservice.application.domain.entity.FriendStatus
import com.example.memberservice.application.domain.entity.Role
import com.example.memberservice.application.usecase.result.FriendResult
import com.example.memberservice.application.usecase.result.MemberResult

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
        /** 내 친구 한 명: 친구 회원에 내가 정한 이름·즐겨찾기·상태를 더한다. */
        fun from(friend: FriendResult): ResponseFriendDto {
            val dto = ResponseFriendDto(MemberDto.of(friend.member))
            dto.friendName = friend.friendName
            dto.favorite = friend.favorite
            dto.status = friend.status
            return dto
        }

        /** 회원 검색 결과(친구 행이 없다): friendName 은 null, 즐겨찾기는 꺼짐, 상태는 NORMAL 로 내려간다. */
        fun of(member: MemberResult): ResponseFriendDto = ResponseFriendDto(MemberDto.of(member))
    }
}
