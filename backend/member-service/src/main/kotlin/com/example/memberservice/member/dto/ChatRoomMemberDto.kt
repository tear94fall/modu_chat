package com.example.memberservice.member.dto

data class ChatRoomMemberDto(
    var chatRoomId: Long? = null,
    var chatRoomMembers: List<MemberDto> = emptyList(),
)
