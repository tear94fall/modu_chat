package com.example.chatservice.member.client;

import com.example.chatservice.member.dto.ChatRoomMemberDto;
import com.example.chatservice.member.dto.MemberDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@FeignClient("member-service")
public interface MemberFeignClient {

    @GetMapping("/member/id/{userId}")
    MemberDto getMember(@Valid @PathVariable("userId") String userId);

    @GetMapping("/member/members")
    List<MemberDto> getMembersByUserId(@Valid @RequestParam("userIds") List<String> userIds);

    @GetMapping("/member/members/{ids}")
    List<MemberDto> getMembersById(@Valid @PathVariable("ids") List<Long> ids);

    @PutMapping("/member/invite")
    List<MemberDto> inviteChatRoom(@Valid @RequestBody ChatRoomMemberDto inviteMemberDto);

    @PutMapping("/member/exit")
    List<MemberDto> exitChatRoom(@Valid @RequestBody ChatRoomMemberDto exitMemberDto);
}

