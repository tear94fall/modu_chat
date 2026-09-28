package com.example.pointservice.api.admin

import com.example.pointservice.api.dto.AdjustRequestDto
import com.example.pointservice.api.dto.AdminPointAccountDto
import com.example.pointservice.api.dto.PointBalanceDto
import com.example.pointservice.api.dto.PointRuleCreateDto
import com.example.pointservice.api.dto.PointRuleDto
import com.example.pointservice.api.dto.PointRuleUpdateDto
import com.example.pointservice.api.dto.PointTransactionDto
import com.example.pointservice.member.MemberSummaryDto
import com.example.pointservice.point.service.PointService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 백오피스가 게이트웨이(ROLE_ADMIN JWT)를 거쳐 부른다. InternalApiFilter 가 토큰을 검사한다. */
@Tag(
    name = "포인트 관리 (어드민)",
    description = "어드민 콘솔용. 게이트웨이가 직원 토큰(ROLE_ADMIN, aud modu-admin)을 확인하고 X-Internal-Token 을 붙인다.",
)
@RestController
@RequestMapping("/api-admin/point")
class PointAdminController(private val pointService: PointService) {

    @Operation(
        summary = "포인트 계정 목록 조회",
        description = "포인트 계정을 최근 변경 순(updatedDate, id 내림차순)으로 돌려준다. " +
            "keyword 가 있으면 member-service 회원 검색(이름·이메일·userId)에 걸린 회원의 계정만 보여 준다. " +
            "이름·이메일은 member-service 에서 붙이며, 조회가 안 되면 그 칸만 null 이다.",
    )
    @GetMapping("/accounts")
    fun accounts(
        @Parameter(description = "회원 검색어(이름·이메일·userId). 비우면 전체", example = "soyul")
        @RequestParam(value = "keyword", required = false) keyword: String?,
        @Parameter(description = "페이지 번호(0부터, 기본 0)", example = "0")
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기(기본 20, 최대 100)", example = "20")
        @RequestParam(value = "size", defaultValue = "20") size: Int,
    ): ResponseEntity<Page<AdminPointAccountDto>> =
        ResponseEntity.ok(
            pointService.accounts(keyword, PageRequest.of(maxOf(page, 0), minOf(maxOf(size, 1), 100), Sort.by(Sort.Order.desc("updatedDate"), Sort.Order.desc("id")))),
        )

    @Operation(
        summary = "포인트 계정 조회",
        description = "한 회원의 포인트 계정(잔액, 이름·이메일)을 돌려준다. 계정이 아직 없으면(한 번도 적립·사용하지 않았으면) 404.",
    )
    @GetMapping("/accounts/{userId}")
    fun account(
        @Parameter(description = "회원 userId", example = "11")
        @PathVariable("userId") userId: String,
    ): ResponseEntity<AdminPointAccountDto> =
        ResponseEntity.ok(pointService.account(userId))

    /** 계정이 없어도 답한다(회원이 없으면 404). 상세 화면 머리글용. */
    @Operation(
        summary = "계정 주인 회원 정보 조회",
        description = "포인트 계정이 없어도 회원 이름·이메일을 돌려준다(상세 화면 머리글용). " +
            "member-service 에 회원이 없거나 응답하지 않으면 404.",
    )
    @GetMapping("/accounts/{userId}/member")
    fun member(
        @Parameter(description = "회원 userId", example = "11")
        @PathVariable("userId") userId: String,
    ): ResponseEntity<MemberSummaryDto> =
        pointService.member(userId)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    @Operation(
        summary = "회원 포인트 원장 조회",
        description = "한 회원의 원장(적립·사용·환불·조정 줄)을 최근 순으로 돌려준다. 계정이 없으면 빈 페이지.",
    )
    @GetMapping("/accounts/{userId}/history")
    fun history(
        @Parameter(description = "회원 userId", example = "11")
        @PathVariable("userId") userId: String,
        @Parameter(description = "페이지 번호(0부터, 기본 0)", example = "0")
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기(기본 20, 최대 100)", example = "20")
        @RequestParam(value = "size", defaultValue = "20") size: Int,
    ): ResponseEntity<Page<PointTransactionDto>> =
        ResponseEntity.ok(pointService.history(userId, PageRequest.of(maxOf(page, 0), minOf(maxOf(size, 1), 100))))

    /** 수동 지급(양수)·회수(음수). 메모는 필수 — 원장에 왜 조정했는지 남는다. */
    @Operation(
        summary = "포인트 수동 조정",
        description = "양수는 지급, 음수는 회수다. 원장에 ADJUST 줄과 메모가 남고 조정 후 잔액을 돌려준다. " +
            "계정이 없으면 만든다. 금액이 0 이면 400, 회수가 잔액보다 크면 409(INSUFFICIENT_POINT).",
    )
    @PostMapping("/accounts/{userId}/adjust")
    fun adjust(
        @Parameter(description = "회원 userId", example = "11")
        @PathVariable("userId") userId: String,
        @Valid @RequestBody request: AdjustRequestDto,
    ): ResponseEntity<PointBalanceDto> =
        ResponseEntity.ok(pointService.adjust(userId, request))

    @Operation(summary = "적립 규칙 목록 조회", description = "모든 적립 규칙(비활성 포함)을 코드 순으로 돌려준다.")
    @GetMapping("/rules")
    fun rules(): ResponseEntity<List<PointRuleDto>> = ResponseEntity.ok(pointService.rules())

    /** 새 규칙. 같은 코드가 있으면 409. */
    @Operation(
        summary = "적립 규칙 추가",
        description = "새 적립 규칙을 만들고 201 로 돌려준다. 다른 서비스는 이 코드로 적립을 요청한다. " +
            "코드 형식이 틀리면 400, 같은 코드가 있으면 409.",
    )
    @PostMapping("/rules")
    fun createRule(@Valid @RequestBody request: PointRuleCreateDto): ResponseEntity<PointRuleDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(pointService.createRule(request))

    /** 규칙 삭제. 이력은 남는다. 출석 규칙은 409. */
    @Operation(
        summary = "적립 규칙 삭제",
        description = "규칙을 지우고 204 로 답한다. 원장은 규칙 코드를 문자열로 들고 있어 과거 이력은 남는다. " +
            "출석 규칙(DAILY_CHECKIN)은 지울 수 없어 409(대신 비활성으로 바꾼다), 없는 규칙이면 404.",
    )
    @DeleteMapping("/rules/{code}")
    fun deleteRule(
        @Parameter(description = "규칙 코드", example = "INVITE_FRIEND")
        @PathVariable("code") code: String,
    ): ResponseEntity<Void> {
        pointService.deleteRule(code)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "적립 규칙 수정",
        description = "규칙의 이름·점수·하루/전체 상한·활성 여부를 바꾼다(코드는 못 바꾼다). " +
            "바뀐 값은 다음 적립부터 적용된다. 없는 규칙이면 404.",
    )
    @PutMapping("/rules/{code}")
    fun updateRule(
        @Parameter(description = "규칙 코드", example = "DAILY_CHECKIN")
        @PathVariable("code") code: String,
        @Valid @RequestBody request: PointRuleUpdateDto,
    ): ResponseEntity<PointRuleDto> =
        ResponseEntity.ok(pointService.updateRule(code, request))
}
