package com.example.chatservice.member.client;

import com.example.chatservice.member.entity.Member;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.util.List;

@FeignClient("member-service")
public interface MemberFeignClient {

    @GetMapping("/member")
    Member getMember(@Valid @RequestParam("id") String userId);

    @GetMapping("/members")
    List<Member> getMembers(@Valid @RequestParam("ids") List<String> userIds);
}

