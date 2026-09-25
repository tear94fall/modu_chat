package com.example.memberservice.api.staff

import com.example.memberservice.member.repository.MemberSort
import com.example.memberservice.staff.StaffEntryDto
import com.example.memberservice.staff.StaffException
import com.example.memberservice.staff.StaffMemberDetailDto
import com.example.memberservice.staff.StaffMemberSummaryDto
import com.example.memberservice.staff.StaffPermissionRequest
import com.example.memberservice.staff.StaffService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 모두 인터널의 회원 조회. 게이트웨이가 ROLE_INTERNAL 을 확인하고 내부 토큰을 붙인다(InternalApiFilter 가 검사).
 */
@RestController
@RequestMapping("/api-staff/member")
class StaffMemberController(private val staffService: StaffService) {

    /** 정렬 규칙은 백오피스 회원 목록과 같다. staffOnly=true 면 직원만. */
    @GetMapping
    fun search(
        @RequestParam(value = "keyword", required = false) keyword: String?,
        @RequestParam(value = "sort", defaultValue = "name,asc") sort: String,
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "20") size: Int,
        @RequestParam(value = "staffOnly", defaultValue = "false") staffOnly: Boolean,
    ): Page<StaffMemberSummaryDto> {
        val memberSort = MemberSort.parse(sort) ?: throw StaffException(HttpStatus.BAD_REQUEST, "지원하지 않는 정렬입니다: $sort")
        return staffService.searchMembers(keyword, memberSort, PageRequest.of(maxOf(page, 0), minOf(maxOf(size, 1), 100)), staffOnly)
    }

    @GetMapping("/{id}")
    fun detail(@PathVariable("id") id: Long): StaffMemberDetailDto = staffService.memberDetail(id)
}

/**
 * 직원 지정·권한 변경. 게이트웨이가 ROLE_SUPER(최상위 관리자)만 통과시킨다. 누가 바꿨는지는 X-Auth-User-Id 로 안다.
 *
 * `/api-staff` 아래에 두지 않는다. 톰캣이 `//` 를 하나로 합치므로 `/api-staff//staff` 가 인터널 권한 라우트를 지나
 * 여기에 닿을 수 있다. 계층 자체를 나누면 게이트웨이의 첫 경로 조각만으로 권한이 갈린다.
 */
@RestController
@RequestMapping("/api-super/staff")
class StaffController(private val staffService: StaffService) {

    @GetMapping
    fun list(): List<StaffEntryDto> = staffService.list()

    @PutMapping("/{memberId}")
    fun set(
        @RequestHeader(value = USER_HEADER, required = false) actor: String?,
        @PathVariable("memberId") memberId: Long,
        @RequestBody request: StaffPermissionRequest,
    ): StaffEntryDto = staffService.setPermissions(requireActor(actor), memberId, request.permissions)

    @DeleteMapping("/{memberId}")
    fun remove(
        @RequestHeader(value = USER_HEADER, required = false) actor: String?,
        @PathVariable("memberId") memberId: Long,
    ): ResponseEntity<Void> {
        staffService.remove(requireActor(actor), memberId)
        return ResponseEntity.noContent().build()
    }

    private fun requireActor(actor: String?): String =
        actor?.takeIf { it.isNotBlank() } ?: throw StaffException(HttpStatus.UNAUTHORIZED, "누가 요청했는지 알 수 없습니다.")

    companion object {
        const val USER_HEADER = "X-Auth-User-Id"
    }
}

/** 직원 API 의 거절을 `{status, error, message}` 로 돌려준다. 콘솔이 message 를 보여 준다. */
@RestControllerAdvice(assignableTypes = [StaffMemberController::class, StaffController::class])
class StaffExceptionAdvice {
    @ExceptionHandler(StaffException::class)
    fun handle(e: StaffException): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(e.status).body(mapOf("status" to e.status.value(), "error" to e.status.reasonPhrase, "message" to e.message))
}
