package com.example.chatservice.member.dto

import com.example.chatservice.chat.dto.ChatRoomDto

class ChatRoomMemberDto(
    val chatRoomId: Long?,
    val chatRoomMembers: List<MemberDto>,
) {
    constructor(chatRoom: ChatRoomDto, members: List<MemberDto>) : this(chatRoom.id, members)
}
