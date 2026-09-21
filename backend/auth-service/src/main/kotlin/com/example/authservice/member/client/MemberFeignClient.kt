package com.example.authservice.member.client

import com.example.authservice.member.dto.GoogleAccountDto
import com.example.authservice.member.dto.MemberDto
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient("member-service")
interface MemberFeignClient {

    @GetMapping("/api-internal/member/id/{userId}")
    fun getMember(@PathVariable("userId") userId: String): MemberDto?

    @GetMapping("/api-internal/member/by-email/{email}")
    fun getMemberByEmail(@PathVariable("email") email: String): MemberDto?

    /** 검증된 구글 계정으로 회원을 찾거나 만든다(이메일 기준). */
    @PostMapping("/api-internal/member/google")
    fun googleMember(@RequestBody account: GoogleAccountDto): MemberDto
}
