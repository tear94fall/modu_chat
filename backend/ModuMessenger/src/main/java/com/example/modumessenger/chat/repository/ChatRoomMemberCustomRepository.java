package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.ChatRoomMember;

public interface ChatRoomMemberCustomRepository {

    ChatRoomMember findByUserIdAndRoomId(String userId, String roomId);

    ChatRoomMember findByMemberUserId(String userId);

    ChatRoomMember findByRoomByRoomId(String roomId);
}
