package com.example.memberservice.api.dto

import com.example.memberservice.application.usecase.command.ChatRoomMembersCommand
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "채팅방 참여·나가기 기록 요청(chat-service → member-service).")
data class ChatRoomMemberDto(
    @field:Schema(description = "채팅방 id. 필수.", example = "42")
    var chatRoomId: Long? = null,
    @field:Schema(description = "대상 회원 목록. 각 항목의 id(member.id)만 쓴다.")
    var chatRoomMembers: List<MemberDto> = emptyList(),
) {
    fun toCommand() = ChatRoomMembersCommand(chatRoomId!!, chatRoomMembers.mapNotNull { it.id })
}
