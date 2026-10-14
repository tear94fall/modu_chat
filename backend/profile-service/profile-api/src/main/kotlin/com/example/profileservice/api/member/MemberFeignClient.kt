package com.example.profileservice.api.member

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

/** member-service 응답 중 쓰는 부분만. 모르는 필드는 무시된다. */
data class MemberIdDto(
    var id: Long? = null,
    var userId: String? = null,
)

@FeignClient(name = "member-service", url = "\${modu.services.member-service}")
interface MemberFeignClient {

    @PostMapping("/api-internal/member/profile/profile")
    fun addMemberProfile(@RequestBody addProfileDto: AddProfileDto): ResponseEntity<Long>

    @GetMapping("/api-internal/member/id/{userId}")
    fun getMember(@PathVariable("userId") userId: String): MemberIdDto
}
