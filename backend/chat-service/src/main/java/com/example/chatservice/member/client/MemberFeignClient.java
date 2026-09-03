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
}

