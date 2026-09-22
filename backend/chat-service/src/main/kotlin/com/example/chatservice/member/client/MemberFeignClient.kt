package com.example.chatservice.member.client

import com.example.chatservice.member.dto.ChatRoomMemberDto
import com.example.chatservice.member.dto.MemberDto
import jakarta.validation.Valid
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@FeignClient("member-service")
interface MemberFeignClient {

    @GetMapping("/api-internal/member/id/{userId}")
    fun getMember(@Valid @PathVariable("userId") userId: String): MemberDto

    @GetMapping("/api-internal/member/members")
    fun getMembersByUserId(@Valid @RequestParam("userIds") userIds: List<String>): List<MemberDto>

    @GetMapping("/api-internal/member/members/{ids}")
    fun getMembersById(@Valid @PathVariable("ids") ids: List<Long>): List<MemberDto>

    @PutMapping("/api-internal/member/invite")
    fun inviteChatRoom(@Valid @RequestBody inviteMemberDto: ChatRoomMemberDto): List<MemberDto>

    @PutMapping("/api-internal/member/exit")
    fun exitChatRoom(@Valid @RequestBody exitMemberDto: ChatRoomMemberDto): List<MemberDto>

    /**
     * userId(구글 sub)가 차단한 사람들의 userId. 1:1 방 이력·미읽음에서 뺄 때 쓴다.
     * X-Internal-Token 은 InternalApiFeignConfig 의 인터셉터가 모든 Feign 호출에 붙인다.
     */
    @GetMapping("/api-internal/member/{userId}/blocked-ids")
    fun getBlockedIds(@PathVariable("userId") userId: String): List<String?>?
}
