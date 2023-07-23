package com.example.chatservice.member.dto;

import com.example.chatservice.chat.dto.ChatRoomDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChatRoomMemberDto {

    private Long chatRoomId;
    private List<MemberDto> chatRoomMembers;

    public ChatRoomMemberDto(ChatRoomDto chatRoom, List<MemberDto> members) {
        this.chatRoomId = chatRoom.getId();
        this.chatRoomMembers = members;
    }
}
