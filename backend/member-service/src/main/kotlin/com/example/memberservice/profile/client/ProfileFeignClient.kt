package com.example.memberservice.profile.client

import com.example.memberservice.profile.dto.ProfileDto
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient("profile-service")
interface ProfileFeignClient {

    @Retry(name = "memberProfileRetry", fallbackMethod = "retryGetMemberProfileFallback")
    @GetMapping("/api-internal/profile/{memberId}")
    fun getMemberProfiles(@PathVariable("memberId") memberId: Long): ResponseEntity<List<ProfileDto>>

    @PostMapping("/api-internal/profile")
    fun addProfileRequest(@RequestBody profileDto: ProfileDto): ResponseEntity<ProfileDto>

    fun retryGetMemberProfileFallback(e: Exception): ResponseEntity<List<ProfileDto>> = ResponseEntity.ok(ArrayList())
}
