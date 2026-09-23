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
@RestController
@RequestMapping("/api-admin/point")
class PointAdminController(private val pointService: PointService) {

    @GetMapping("/accounts")
    fun accounts(
        @RequestParam(value = "keyword", required = false) keyword: String?,
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "20") size: Int,
    ): ResponseEntity<Page<AdminPointAccountDto>> =
        ResponseEntity.ok(
            pointService.accounts(keyword, PageRequest.of(maxOf(page, 0), minOf(maxOf(size, 1), 100), Sort.by(Sort.Order.desc("updatedDate"), Sort.Order.desc("id")))),
        )

    @GetMapping("/accounts/{userId}")
    fun account(@PathVariable("userId") userId: String): ResponseEntity<AdminPointAccountDto> =
        ResponseEntity.ok(pointService.account(userId))

    /** 계정이 없어도 답한다(회원이 없으면 404). 상세 화면 머리글용. */
    @GetMapping("/accounts/{userId}/member")
    fun member(@PathVariable("userId") userId: String): ResponseEntity<MemberSummaryDto> =
        pointService.member(userId)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    @GetMapping("/accounts/{userId}/history")
    fun history(
        @PathVariable("userId") userId: String,
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "20") size: Int,
    ): ResponseEntity<Page<PointTransactionDto>> =
        ResponseEntity.ok(pointService.history(userId, PageRequest.of(maxOf(page, 0), minOf(maxOf(size, 1), 100))))

    /** 수동 지급(양수)·회수(음수). 메모는 필수 — 원장에 왜 조정했는지 남는다. */
    @PostMapping("/accounts/{userId}/adjust")
    fun adjust(@PathVariable("userId") userId: String, @Valid @RequestBody request: AdjustRequestDto): ResponseEntity<PointBalanceDto> =
        ResponseEntity.ok(pointService.adjust(userId, request))

    @GetMapping("/rules")
    fun rules(): ResponseEntity<List<PointRuleDto>> = ResponseEntity.ok(pointService.rules())

    /** 새 규칙. 같은 코드가 있으면 409. */
    @PostMapping("/rules")
    fun createRule(@Valid @RequestBody request: PointRuleCreateDto): ResponseEntity<PointRuleDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(pointService.createRule(request))

    /** 규칙 삭제. 이력은 남는다. 출석 규칙은 409. */
    @DeleteMapping("/rules/{code}")
    fun deleteRule(@PathVariable("code") code: String): ResponseEntity<Void> {
        pointService.deleteRule(code)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/rules/{code}")
    fun updateRule(@PathVariable("code") code: String, @Valid @RequestBody request: PointRuleUpdateDto): ResponseEntity<PointRuleDto> =
        ResponseEntity.ok(pointService.updateRule(code, request))
}
