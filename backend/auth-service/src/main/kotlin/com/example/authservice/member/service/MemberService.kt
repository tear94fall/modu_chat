package com.example.authservice.member.service

import com.example.authservice.member.client.MemberFeignClient
import com.example.authservice.member.dto.MemberDto
import org.springframework.stereotype.Service

@Service
class MemberService(private val memberFeignClient: MemberFeignClient) {

    fun getMember(userId: String): MemberDto? = memberFeignClient.getMember(userId)
}
