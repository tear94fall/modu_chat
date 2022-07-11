package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.ChatRoomMember;

import java.util.List;

public interface ChatRoomMemberCustomRepository {

    ChatRoomMember findByUserIdAndRoomId(String userId, String roomId);

    List<ChatRoomMember> findAllByMemberUserId(String userId);

    ChatRoomMember findByMemberUserId(String userId);

    ChatRoomMember findByRoomByRoomId(String roomId);
}
