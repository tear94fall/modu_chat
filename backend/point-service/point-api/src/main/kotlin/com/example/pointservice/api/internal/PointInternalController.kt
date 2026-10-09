package com.example.pointservice.api.internal

import com.example.pointservice.api.dto.EarnAmountRequestDto
import com.example.pointservice.api.dto.EarnRequestDto
import com.example.pointservice.api.dto.EarnResultDto
import com.example.pointservice.api.dto.PointBalanceDto
import com.example.pointservice.api.dto.PointRefTransactionDto
import com.example.pointservice.api.dto.PointRefsRequestDto
import com.example.pointservice.api.dto.PointRefsResultDto
import com.example.pointservice.api.dto.PointTransactionDto
import com.example.pointservice.api.dto.SpendCancelRequestDto
import com.example.pointservice.api.dto.SpendCancelResultDto
import com.example.pointservice.api.dto.SpendRequestDto
import com.example.pointservice.api.dto.SpendResultDto
import com.example.pointservice.application.usecase.PointUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(
    name = "포인트 (내부)",
    description = "서비스끼리만 호출(member·chat·commerce 가 Feign 으로). X-Internal-Token 필요, 게이트웨이로는 열려 있지 않다.",
)
@RestController
@RequestMapping("/api-internal/point")
class PointInternalController(private val pointUseCase: PointUseCase) {

    /** 규칙 코드로 적립. 중복·상한이면 applied=false(200). 모르는 규칙이면 404. */
    @Operation(
        summary = "규칙 코드로 포인트 적립",
        description = "점수는 규칙이 정하므로 규칙 코드만 보낸다. 모르는 규칙이면 404. " +
            "비활성 규칙·같은 (userId, refId) 중복·전체 상한·하루 상한(한국 시간 기준)에 걸리면 오류 대신 200 에 applied=false 와 " +
            "reason(RULE_DISABLED, DUPLICATE, TOTAL_LIMIT, DAILY_LIMIT)을 준다 — 호출 쪽이 재시도해도 안전하다. 계정이 없으면 만든다.",
    )
    @PostMapping("/earn")
    fun earn(@Valid @RequestBody request: EarnRequestDto): ResponseEntity<EarnResultDto> =
        ResponseEntity.ok(EarnResultDto.of(pointUseCase.earn(request.toCommand())))

    /** 금액 지정 적립(구매 적립 등). 규칙·상한 없음. 같은 (userId, refId) 로 다시 오면 applied=false, reason=DUPLICATE(200). */
    @Operation(
        summary = "금액 지정 포인트 적립",
        description = "규칙 없이 정한 금액을 적립한다(구매 적립 등). 상한은 없고 출처 reason 이 원장에 남는다. " +
            "같은 (userId, refId) 로 다시 오면 적립하지 않고 200 에 applied=false, reason=DUPLICATE. 금액이 1~1,000,000 밖이면 400.",
    )
    @PostMapping("/earn-amount")
    fun earnAmount(@Valid @RequestBody request: EarnAmountRequestDto): ResponseEntity<EarnResultDto> =
        ResponseEntity.ok(EarnResultDto.of(pointUseCase.earnAmount(request.toCommand())))

    /** 사용(차감). 부족하면 409 INSUFFICIENT_POINT. 같은 refId 로 다시 오면 applied=false. */
    @Operation(
        summary = "포인트 사용",
        description = "잔액에서 amount 만큼 차감하고 원장에 SPEND 줄을 남긴다(주문 결제 등). " +
            "같은 (userId, refId) 가 원장에 이미 있으면 차감하지 않고 applied=false 와 현재 잔액을 준다. " +
            "amount 가 0 이하면 400, 잔액이 모자라면 409(INSUFFICIENT_POINT).",
    )
    @PostMapping("/spend")
    fun spend(@Valid @RequestBody request: SpendRequestDto): ResponseEntity<SpendResultDto> =
        ResponseEntity.ok(SpendResultDto.of(pointUseCase.spend(request.toCommand())))

    /** 사용 취소(환불). 같은 refId 로 다시 오면 applied=false. */
    @Operation(
        summary = "포인트 사용 취소(환불)",
        description = "주문 취소 등으로 앞서 차감한 포인트를 돌려주고 원장에 REFUND 줄을 남긴다. " +
            "refId 는 원장 전체에서 사용자별로 한 번만 쓰이므로 사용 때와 다른 키(예: refund:order:…)를 보낸다. " +
            "같은 refId 로 다시 오면 돌려주지 않고 applied=false. amount 가 0 이하면 400.",
    )
    @PostMapping("/refund")
    fun refund(@Valid @RequestBody request: SpendRequestDto): ResponseEntity<SpendResultDto> =
        ResponseEntity.ok(SpendResultDto.of(pointUseCase.refund(request.toCommand())))

    /** 사용 취소: 사용했으면 되돌린다. 사용이 없거나 이미 환불됐으면 cancelled=false(200). */
    @Operation(
        summary = "포인트 사용 취소(사용했을 때만)",
        description = "원래 사용(SPEND, 같은 userId·refId)이 원장에 있을 때만 그 금액을 돌려주고 REFUND 줄을 refId \"refund:\" + refId 로 남긴다. " +
            "커머스 주문 취소 환불(refund:order:…)과 같은 키라 두 경로 중 어느 쪽으로 와도 한 번만 돌려준다. " +
            "사용이 없으면 200 에 cancelled=false, reason=NO_SPEND(원장에 아무것도 쓰지 않는다). " +
            "이미 환불됐으면 cancelled=false, reason=ALREADY_REFUNDED. 금액은 보내지 않는다(원장의 사용 금액을 쓴다). " +
            "계정을 잠그고 처리하므로 동시에 여러 번 와도 한 번만 돌려준다 — 결제 실패·타임아웃 뒤 정리용으로 안전하게 재시도할 수 있다.",
    )
    @PostMapping("/spend/cancel")
    fun cancelSpend(@Valid @RequestBody request: SpendCancelRequestDto): ResponseEntity<SpendCancelResultDto> =
        ResponseEntity.ok(SpendCancelResultDto.of(pointUseCase.cancelSpend(request.toCommand())))

    /** (userId, refId) 쌍으로 원장 줄 찾기. 커머스 야간 대사용. 레플리카에서 읽는다. */
    @Operation(
        summary = "refId 로 원장 줄 찾기(대사)",
        description = "보낸 (userId, refId) 쌍 중 원장에 있는 줄만 type·amount(부호 있음, 사용은 음수)·createdDate 와 함께 돌려준다. " +
            "없는 쌍은 결과에서 빠진다. 한 번에 최대 500쌍(넘으면 400). 커머스 야간 대사용이라 레플리카에서 읽는다 — " +
            "방금 쓴 줄은 아직 안 보일 수 있다.",
    )
    @PostMapping("/refs")
    fun transactionsByRefs(@Valid @RequestBody request: PointRefsRequestDto): ResponseEntity<PointRefsResultDto> =
        ResponseEntity.ok(
            PointRefsResultDto(pointUseCase.transactionsByRefs(request.refs.map { it.toRef() }).map { PointRefTransactionDto.of(it) }),
        )

    @Operation(summary = "포인트 잔액 조회", description = "회원의 현재 잔액을 돌려준다. 계정이 없으면 0. 적립·사용 직후에 불러도 방금 값이 나온다(master 에서 읽는다).")
    @GetMapping("/{userId}/balance")
    fun balance(
        @Parameter(description = "회원 userId", example = "11")
        @PathVariable("userId") userId: String,
    ): ResponseEntity<PointBalanceDto> =
        ResponseEntity.ok(PointBalanceDto.of(pointUseCase.balance(userId)))

    /** 최근 순 원장. 커머스처럼 게이트웨이 공개 라우트(aud=modu-chat)를 못 쓰는 서비스가 자기 사용자 대신 조회한다. */
    @Operation(
        summary = "포인트 원장 조회",
        description = "회원의 원장을 최근 순으로 돌려준다. 커머스처럼 게이트웨이 공개 라우트(aud modu-chat)를 못 쓰는 서비스가 " +
            "자기 사용자 대신 조회할 때 쓴다. 계정이 없으면 빈 페이지.",
    )
    @GetMapping("/{userId}/history")
    fun history(
        @Parameter(description = "회원 userId", example = "11")
        @PathVariable("userId") userId: String,
        @Parameter(description = "페이지 번호(0부터, 기본 0)", example = "0")
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기(기본 20, 최대 100)", example = "20")
        @RequestParam(value = "size", defaultValue = "20") size: Int,
    ): ResponseEntity<Page<PointTransactionDto>> =
        ResponseEntity.ok(pointUseCase.history(userId, PageRequest.of(maxOf(page, 0), minOf(maxOf(size, 1), 100))).map { PointTransactionDto.of(it) })
}
