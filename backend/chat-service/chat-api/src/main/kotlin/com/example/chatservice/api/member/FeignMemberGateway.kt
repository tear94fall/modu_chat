package com.example.chatservice.api.member

import com.example.chatservice.api.dto.MemberDto
import com.example.chatservice.application.member.MemberGateway
import com.example.chatservice.application.member.MemberInfo
import org.springframework.stereotype.Component

/** [MemberGateway] 의 Feign 구현. 실패는 삼키지 않는다 — 삼킬지는 유스케이스·캐시가 정한다. */
@Component
class FeignMemberGateway(private val memberFeignClient: MemberFeignClient) : MemberGateway {

    override fun byUserId(userId: String): MemberInfo = memberFeignClient.getMember(userId).toInfo()

    override fun byUserIds(userIds: List<String>): List<MemberInfo> =
        memberFeignClient.getMembersByUserId(userIds).map { it.toInfo() }

    override fun byIds(ids: List<Long>): List<MemberInfo> = memberFeignClient.getMembersById(ids).map { it.toInfo() }

    override fun invite(chatRoomId: Long?, members: List<MemberInfo>): List<MemberInfo> =
        memberFeignClient.inviteChatRoom(ChatRoomMemberDto(chatRoomId, members.map { MemberDto.of(it) })).map { it.toInfo() }

    override fun exit(chatRoomId: Long?, members: List<MemberInfo>): List<MemberInfo> =
        memberFeignClient.exitChatRoom(ChatRoomMemberDto(chatRoomId, members.map { MemberDto.of(it) })).map { it.toInfo() }

    override fun blockedIds(userId: String): List<String?>? = memberFeignClient.getBlockedIds(userId)
}
