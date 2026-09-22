package com.example.chatservice.chat.repository

import com.example.chatservice.chat.entity.ChatRoomMember
import java.util.Optional

interface ChatRoomMemberCustomRepository {

    fun findAllByMemberId(memberId: Long): List<ChatRoomMember>

    /** 멤버 id 집합이 정확히 일치하는 방의 PK. 없으면 empty. */
    fun findRoomIdByExactMemberIds(memberIds: Set<Long>): Optional<Long>
}
