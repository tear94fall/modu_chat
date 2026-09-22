package com.example.profileservice.member.client

import com.example.profileservice.member.dto.AddProfileDto
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient("member-service")
interface MemberFeignClient {

    @PostMapping("/api-internal/member/profile/profile")
    fun addMemberProfile(@RequestBody addProfileDto: AddProfileDto): ResponseEntity<Long>
}
