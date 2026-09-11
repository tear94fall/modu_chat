package com.example.authservice.member.client;

import com.example.authservice.member.dto.GoogleAccountDto;
import com.example.authservice.member.dto.MemberDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("member-service")
public interface MemberFeignClient {

    @GetMapping(value = "/api-internal/member/id/{userId}")
    MemberDto getMember(@PathVariable("userId") String userId);

    @GetMapping(value = "/api-internal/member/by-email/{email}")
    MemberDto getMemberByEmail(@PathVariable("email") String email);

    /** 검증된 구글 계정으로 회원을 찾거나 만든다(이메일 기준). */
    @PostMapping(value = "/api-internal/member/google")
    MemberDto googleMember(@RequestBody GoogleAccountDto account);
}
