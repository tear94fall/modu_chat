package com.example.pointservice.api.internal

import com.example.pointservice.api.dto.EarnRequestDto
import com.example.pointservice.api.dto.EarnResultDto
import com.example.pointservice.api.dto.PointBalanceDto
import com.example.pointservice.api.dto.PointTransactionDto
import com.example.pointservice.api.dto.SpendRequestDto
import com.example.pointservice.api.dto.SpendResultDto
import com.example.pointservice.point.service.PointService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 다른 서비스(member, chat, commerce)가 Feign 으로 부른다. InternalApiFilter 가 X-Internal-Token 을 검사한다. */
@RestController
@RequestMapping("/api-internal/point")
class PointInternalController(private val pointService: PointService) {

    /** 규칙 코드로 적립. 중복·상한이면 applied=false(200). 모르는 규칙이면 404. */
    @PostMapping("/earn")
    fun earn(@Valid @RequestBody request: EarnRequestDto): ResponseEntity<EarnResultDto> =
        ResponseEntity.ok(pointService.earn(request.userId, request.ruleCode, request.refId, request.memo))

    /** 사용(차감). 부족하면 409 INSUFFICIENT_POINT. 같은 refId 로 다시 오면 applied=false. */
    @PostMapping("/spend")
    fun spend(@Valid @RequestBody request: SpendRequestDto): ResponseEntity<SpendResultDto> =
        ResponseEntity.ok(pointService.spend(request.userId, request.amount, request.refId, request.memo))

    /** 사용 취소(환불). 같은 refId 로 다시 오면 applied=false. */
    @PostMapping("/refund")
    fun refund(@Valid @RequestBody request: SpendRequestDto): ResponseEntity<SpendResultDto> =
        ResponseEntity.ok(pointService.refund(request.userId, request.amount, request.refId, request.memo))

    @GetMapping("/{userId}/balance")
    fun balance(@PathVariable("userId") userId: String): ResponseEntity<PointBalanceDto> =
        ResponseEntity.ok(pointService.balance(userId))

    /** 최근 순 원장. 커머스처럼 게이트웨이 공개 라우트(aud=modu-chat)를 못 쓰는 서비스가 자기 사용자 대신 조회한다. */
    @GetMapping("/{userId}/history")
    fun history(
        @PathVariable("userId") userId: String,
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "20") size: Int,
    ): ResponseEntity<Page<PointTransactionDto>> =
        ResponseEntity.ok(pointService.history(userId, PageRequest.of(maxOf(page, 0), minOf(maxOf(size, 1), 100))))
}
