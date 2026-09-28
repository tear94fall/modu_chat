package com.example.memberservice.api.staff

import com.example.memberservice.member.repository.MemberSort
import com.example.memberservice.staff.StaffEntryDto
import com.example.memberservice.staff.StaffException
import com.example.memberservice.staff.StaffMemberDetailDto
import com.example.memberservice.staff.StaffMemberSummaryDto
import com.example.memberservice.staff.StaffPermissionRequest
import com.example.memberservice.staff.StaffService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(
    name = "회원 조회 (직원)",
    description = "모두 인터널 콘솔용. 게이트웨이가 직원 토큰(ROLE_INTERNAL 또는 ROLE_SUPER)을 확인하고 X-Internal-Token 을 붙인다.",
)
@RestController
@RequestMapping("/api-staff/member")
class StaffMemberController(private val staffService: StaffService) {

    /** 정렬 규칙은 백오피스 회원 목록과 같다. staffOnly=true 면 직원만. */
    @Operation(
        summary = "회원 목록 검색",
        description = "이메일·이름으로 회원을 찾아 페이지로 돌려준다(비우면 전체). 각 줄에 직원 권한이 붙고, 비어 있으면 직원이 아니다. " +
            "정렬 규칙은 어드민 회원 목록과 같다(기본 이름 가나다순, 한글 먼저). 모르는 sort 면 400(`{status, error, message}`).",
    )
    @GetMapping
    fun search(
        @Parameter(description = "검색어. 이메일·이름에 들어 있으면(대소문자 무시) 걸린다. 비우면 전체 회원.", example = "소율")
        @RequestParam(value = "keyword", required = false) keyword: String?,
        @Parameter(
            description = "정렬. name | email | userId | role | createdDate 에 ,asc 또는 ,desc(방향 생략 시 asc). 기본 name,asc.",
            example = "createdDate,desc",
        )
        @RequestParam(value = "sort", defaultValue = "name,asc") sort: String,
        @Parameter(description = "페이지 번호(0부터). 기본 0, 음수는 0 으로 본다.", example = "0")
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기. 기본 20, 1~100 으로 맞춘다.", example = "20")
        @RequestParam(value = "size", defaultValue = "20") size: Int,
        @Parameter(description = "true 면 직원만 보여 준다. 기본 false.", example = "false")
        @RequestParam(value = "staffOnly", defaultValue = "false") staffOnly: Boolean,
    ): Page<StaffMemberSummaryDto> {
        val memberSort = MemberSort.parse(sort) ?: throw StaffException(HttpStatus.BAD_REQUEST, "지원하지 않는 정렬입니다: $sort")
        return staffService.searchMembers(keyword, memberSort, PageRequest.of(maxOf(page, 0), minOf(maxOf(size, 1), 100)), staffOnly)
    }

    @Operation(
        summary = "회원 상세 조회",
        description = "회원 한 명의 기본 정보·상태·친구 수와 직원 정보(권한, 지정·변경 시각, 마지막으로 바꾼 사람)를 돌려준다. " +
            "직원이 아니면 staff 가 null. 없는 회원이면 404.",
    )
    @GetMapping("/{id}")
    fun detail(@Parameter(description = "회원 id(member.id)", example = "11") @PathVariable("id") id: Long): StaffMemberDetailDto = staffService.memberDetail(id)
}

/**
 * 직원 지정·권한 변경. 게이트웨이가 ROLE_SUPER(최상위 관리자)만 통과시킨다. 누가 바꿨는지는 X-Auth-User-Id 로 안다.
 *
 * `/api-staff` 아래에 두지 않는다. 톰캣이 `//` 를 하나로 합치므로 `/api-staff//staff` 가 인터널 권한 라우트를 지나
 * 여기에 닿을 수 있다. 계층 자체를 나누면 게이트웨이의 첫 경로 조각만으로 권한이 갈린다.
 */
@Tag(
    name = "직원 권한 (최상위)",
    description = "직원 관리 화면용. 게이트웨이가 최상위 관리자 토큰(ROLE_SUPER)만 통과시키고 X-Internal-Token 을 붙인다.",
)
@RestController
@RequestMapping("/api-super/staff")
class StaffController(private val staffService: StaffService) {

    @Operation(summary = "직원 목록 조회", description = "직원 전부를 이름(없으면 이메일) 가나다순으로 돌려준다. 권한과 마지막으로 바꾼 사람이 함께 온다.")
    @GetMapping
    fun list(): List<StaffEntryDto> = staffService.list()

    @Operation(
        summary = "직원 지정·권한 변경",
        description = "회원을 직원으로 지정하거나 권한 목록을 통째로 바꾼다. 권한이 비면 400(직원에서 빼려면 해제 API), 없는 회원이면 404, " +
            "자기 자신이거나 탈퇴한 회원이면 409, 헤더가 없으면 401. 오류는 `{status, error, message}` 로 돌려준다.",
    )
    @PutMapping("/{memberId}")
    fun set(
        @Parameter(description = "요청한 최상위 관리자 userId. 게이트웨이가 JWT sub 로 넣는다.", example = "112233445566778899001")
        @RequestHeader(value = USER_HEADER, required = false) actor: String?,
        @Parameter(description = "대상 회원 id(member.id)", example = "12")
        @PathVariable("memberId") memberId: Long,
        @RequestBody request: StaffPermissionRequest,
    ): StaffEntryDto = staffService.setPermissions(requireActor(actor), memberId, request.permissions)

    @Operation(
        summary = "직원 해제",
        description = "회원의 직원 지정을 없앤다(회원은 그대로). 직원이 아니면 404, 자기 자신이면 409, 헤더가 없으면 401. 성공하면 204.",
    )
    @DeleteMapping("/{memberId}")
    fun remove(
        @Parameter(description = "요청한 최상위 관리자 userId. 게이트웨이가 JWT sub 로 넣는다.", example = "112233445566778899001")
        @RequestHeader(value = USER_HEADER, required = false) actor: String?,
        @Parameter(description = "대상 회원 id(member.id)", example = "12")
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
