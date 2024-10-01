package com.example.chatservice.chat.repository;

import com.example.chatservice.chat.entity.ChatRoomMember;

import java.util.List;

public interface ChatRoomMemberCustomRepository {

    List<ChatRoomMember> findAllByMemberId(Long memberId);
}
