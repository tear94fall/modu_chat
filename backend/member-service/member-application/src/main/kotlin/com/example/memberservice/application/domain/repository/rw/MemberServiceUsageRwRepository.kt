package com.example.memberservice.application.domain.repository.rw

import com.example.memberservice.application.config.RwRepository
import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.entity.MemberServiceUsage
import com.example.memberservice.application.domain.entity.MemberStatus
import com.example.memberservice.application.domain.entity.ModuService
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MemberServiceUsageRwRepository : RwRepository<MemberServiceUsage, Long> {

    fun findByUserIdAndService(userId: String, service: ModuService): MemberServiceUsage?

    fun findAllByUserId(userId: String): List<MemberServiceUsage>

    fun findAllByUserIdIn(userIds: Collection<String>): List<MemberServiceUsage>

    fun findAllByServiceAndUserIdIn(service: ModuService, userIds: Collection<String>): List<MemberServiceUsage>

    /**
     * 채팅 이용 기록을 채워 넣을 회원: 활동 중이고, 채팅방에 들어가 있거나 친구 관계(어느 쪽이든)가 있으며,
     * 아직 [service] 이용 기록이 없는 회원.
     */
    @Query(
        """
        select m from Member m
        where m.status = :status
          and (m.chatRoomMembers is not empty
               or exists (select f.id from MemberFriend f where f.member = m or f.friend = m))
          and not exists (select u.id from MemberServiceUsage u where u.userId = m.userId and u.service = :service)
        """,
    )
    fun findChatMembersWithoutUsage(
        @Param("status") status: MemberStatus,
        @Param("service") service: ModuService,
    ): List<Member>
}
