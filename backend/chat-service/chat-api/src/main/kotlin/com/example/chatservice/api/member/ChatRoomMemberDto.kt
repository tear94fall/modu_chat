package com.example.chatservice.api.member

import com.example.chatservice.api.dto.MemberDto

/** member-service 의 초대·나가기 요청 본문. chatRoomId 는 방의 PK(숫자)다. */
class ChatRoomMemberDto(
    val chatRoomId: Long?,
    val chatRoomMembers: List<MemberDto>,
)
