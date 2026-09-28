package com.example.memberservice.api.internal

import com.example.memberservice.usage.MemberServiceUsageService
import com.example.memberservice.usage.dto.BulkUsageRequest
import com.example.memberservice.usage.dto.BulkUsageResponse
import com.example.memberservice.usage.dto.UsageRequest
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 서비스 이용 기록. auth-service 가 토큰 발급 뒤 부르고, 백필은 운영자가 한 번 부른다. InternalApiFilter 가 보호한다. */
@RestController
@RequestMapping("/api-internal/member/usage")
class MemberUsageInternalController(private val usageService: MemberServiceUsageService) {

    private val log = LoggerFactory.getLogger(MemberUsageInternalController::class.java)

    /** 모르는 클라이언트·회원이어도 204. 같은 회원의 첫 기록이 동시에 두 번 오면 한쪽은 유일 제약에 걸리는데, 이미 기록됐으므로 204. */
    @PostMapping
    fun record(@RequestBody request: UsageRequest): ResponseEntity<Void> {
        try {
            usageService.record(request.userId, request.clientId)
        } catch (e: DataIntegrityViolationException) {
            log.debug("이용 기록이 동시에 들어와 한쪽을 건너뜀 userId={} clientId={}", request.userId, request.clientId)
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/bulk")
    fun bulk(@RequestBody request: BulkUsageRequest): ResponseEntity<BulkUsageResponse> =
        ResponseEntity.ok(BulkUsageResponse(usageService.bulkInsert(request.service, request.userIds, request.usedAt)))
}
