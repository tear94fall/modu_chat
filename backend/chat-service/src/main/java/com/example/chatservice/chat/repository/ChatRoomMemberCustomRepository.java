package com.example.chatservice.chat.repository;

import com.example.chatservice.chat.entity.ChatRoomMember;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ChatRoomMemberCustomRepository {

    List<ChatRoomMember> findAllByMemberId(Long memberId);

    /** 멤버 id 집합이 정확히 일치하는 방의 PK. 없으면 empty. */
    Optional<Long> findRoomIdByExactMemberIds(Set<Long> memberIds);
}
