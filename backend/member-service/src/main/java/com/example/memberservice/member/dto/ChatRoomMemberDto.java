package com.example.memberservice.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChatRoomMemberDto {

    private Long chatRoomId;
    private List<MemberDto> chatRoomMembers;
}

