package com.example.chatservice.application.domain.repository.rw

import com.example.chatservice.application.domain.entity.ChatRoomMember
import java.util.Optional

interface ChatRoomMemberRwCustomRepository {

    fun findAllByMemberId(memberId: Long): List<ChatRoomMember>

    /** 멤버 id 집합이 정확히 일치하는 방의 PK. 없으면 empty. */
    fun findRoomIdByExactMemberIds(memberIds: Set<Long>): Optional<Long>
}
