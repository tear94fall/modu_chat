package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.ChatRoomMember;

public interface ChatRoomMemberCustomRepository {

    ChatRoomMember findByMemberUserId(String userId);

    ChatRoomMember findByRoomByRoomId(String roomId);
}
