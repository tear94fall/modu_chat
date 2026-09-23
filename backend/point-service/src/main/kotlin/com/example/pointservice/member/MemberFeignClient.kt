package com.example.pointservice.member

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

/** member-service 응답 중 백오피스가 쓰는 부분만. 모르는 필드는 무시된다. */
data class MemberSummaryDto(
    var userId: String? = null,
    var username: String? = null,
    var email: String? = null,
)

data class MemberPageDto(var content: List<MemberSummaryDto> = emptyList())

/** 포인트 계정은 userId 만 알므로 이름·이메일은 member-service 에서 받아 붙인다. */
@FeignClient("member-service")
interface MemberFeignClient {

    @GetMapping("/api-internal/member/members")
    fun getMembersByUserId(@RequestParam("userIds") userIds: List<String>): List<MemberSummaryDto>

    /** 백오피스 회원 검색(이름·이메일·userId). 관리자 API 지만 내부 토큰으로도 통과한다. */
    @GetMapping("/api-admin/member")
    fun searchMembers(
        @RequestParam("keyword") keyword: String,
        @RequestParam("page") page: Int,
        @RequestParam("size") size: Int,
    ): MemberPageDto
}
