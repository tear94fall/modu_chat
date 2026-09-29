package com.example.pointservice.api.pub

import com.example.pointservice.api.common.AuthUserInterceptor.Companion.AUTH_USER_ID_HEADER
import com.example.pointservice.api.dto.EarnResultDto
import com.example.pointservice.api.dto.PointBalanceDto
import com.example.pointservice.api.dto.PointTransactionDto
import com.example.pointservice.application.usecase.PointUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 앱이 게이트웨이를 거쳐 부른다. 게이트웨이가 JWT 를 검증하고 subject(userId)를 X-Auth-User-Id 로 넣어 준다.
 * 헤더가 없는 요청은 AuthUserInterceptor 가 403 으로 막는다 — 여기까지 오면 헤더는 항상 있다.
 */
@Tag(
    name = "포인트 (앱)",
    description = "모두의 채팅 앱이 게이트웨이를 거쳐 부른다. 모두 계정 토큰(aud modu-chat) 필요, 게이트웨이가 X-Auth-User-Id 를 넣어 준다.",
)
@RestController
@RequestMapping("/api-public/point")
class PointPublicController(private val pointUseCase: PointUseCase) {

    @Operation(summary = "내 포인트 잔액 조회", description = "내 현재 잔액을 돌려준다. 계정이 없으면 0. X-Auth-User-Id 가 없으면 403.")
    @GetMapping("/me")
    fun me(
        @Parameter(description = "게이트웨이가 토큰에서 넣어 주는 회원 userId", example = "11")
        @RequestHeader(AUTH_USER_ID_HEADER) userId: String,
    ): ResponseEntity<PointBalanceDto> =
        ResponseEntity.ok(PointBalanceDto.of(pointUseCase.balance(userId)))

    @Operation(
        summary = "내 포인트 내역 조회",
        description = "내 원장(적립·사용·환불·조정)을 최근 순으로 돌려준다. X-Auth-User-Id 가 없으면 403.",
    )
    @GetMapping("/me/history")
    fun history(
        @Parameter(description = "게이트웨이가 토큰에서 넣어 주는 회원 userId", example = "11")
        @RequestHeader(AUTH_USER_ID_HEADER) userId: String,
        @Parameter(description = "페이지 번호(0부터, 기본 0)", example = "0")
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기(기본 20, 최대 100)", example = "20")
        @RequestParam(value = "size", defaultValue = "20") size: Int,
    ): ResponseEntity<Page<PointTransactionDto>> =
        ResponseEntity.ok(
            pointUseCase.history(userId, PageRequest.of(maxOf(page, 0), minOf(maxOf(size, 1), 100))).map { PointTransactionDto.of(it) },
        )

    /** 출석 체크. 오늘 이미 했으면 applied=false 로 답한다(200). */
    @Operation(
        summary = "출석 체크",
        description = "출석 규칙(DAILY_CHECKIN)으로 적립한다. 하루(한국 시간) 한 번이며, 오늘 이미 했거나 규칙이 비활성이면 " +
            "오류 대신 200 에 applied=false 와 reason 을 준다. X-Auth-User-Id 가 없으면 403.",
    )
    @PostMapping("/me/checkin")
    fun checkIn(
        @Parameter(description = "게이트웨이가 토큰에서 넣어 주는 회원 userId", example = "11")
        @RequestHeader(AUTH_USER_ID_HEADER) userId: String,
    ): ResponseEntity<EarnResultDto> =
        ResponseEntity.ok(EarnResultDto.of(pointUseCase.checkIn(userId)))
}
