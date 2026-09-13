package com.example.chatservice.member.client;

import com.example.chatservice.member.dto.ChatRoomMemberDto;
import com.example.chatservice.member.dto.MemberDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@FeignClient("member-service")
public interface MemberFeignClient {

    @GetMapping("/api-internal/member/id/{userId}")
    MemberDto getMember(@Valid @PathVariable("userId") String userId);

    @GetMapping("/api-internal/member/members")
    List<MemberDto> getMembersByUserId(@Valid @RequestParam("userIds") List<String> userIds);

    @GetMapping("/api-internal/member/members/{ids}")
    List<MemberDto> getMembersById(@Valid @PathVariable("ids") List<Long> ids);

    @PutMapping("/api-internal/member/invite")
    List<MemberDto> inviteChatRoom(@Valid @RequestBody ChatRoomMemberDto inviteMemberDto);

    @PutMapping("/api-internal/member/exit")
    List<MemberDto> exitChatRoom(@Valid @RequestBody ChatRoomMemberDto exitMemberDto);

    /**
     * userId(구글 sub)가 차단한 사람들의 userId. 1:1 방 이력·미읽음에서 뺄 때 쓴다.
     * X-Internal-Token 은 InternalApiFeignConfig 의 인터셉터가 모든 Feign 호출에 붙인다.
     */
    @GetMapping("/api-internal/member/{userId}/blocked-ids")
    List<String> getBlockedIds(@PathVariable("userId") String userId);
}

