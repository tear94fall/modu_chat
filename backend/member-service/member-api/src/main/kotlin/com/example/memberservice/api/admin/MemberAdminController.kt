package com.example.memberservice.api.admin

import com.example.memberservice.api.admin.dto.AdminFriendPageDto
import com.example.memberservice.api.admin.dto.AdminMemberDetailDto
import com.example.memberservice.api.admin.dto.AdminMemberSummaryDto
import com.example.memberservice.api.dto.UpdateProfileDto
import com.example.memberservice.application.domain.repository.query.AdminFriendFilter
import com.example.memberservice.application.domain.repository.query.MemberSort
import com.example.memberservice.application.usecase.MemberAdminUseCase
import com.example.memberservice.application.domain.repository.query.ServiceFilter
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** 백오피스가 게이트웨이(ROLE_ADMIN JWT)를 거쳐 부른다. InternalApiFilter 가 토큰을 검사한다. */
@Tag(name = "회원 관리 (어드민)", description = "어드민 콘솔용. 게이트웨이가 직원 토큰(ROLE_ADMIN, aud modu-admin)을 확인하고 X-Internal-Token 을 붙인다.")
@RestController
@RequestMapping("/api-admin/member")
class MemberAdminController(private val memberAdminUseCase: MemberAdminUseCase) {

    /**
     * 백오피스 목록. 기본은 이름 가나다순이고 한글 이름이 영문·숫자보다 먼저 온다 — 앱 친구 목록과 같은 규칙이다.
     * sort 는 [MemberSort] 허용 목록(name | email | userId | role | createdDate 에 ,asc 또는 ,desc)만 받고
     * 모르는 값이면 400 이다. 조용히 기본 정렬로 되돌리면 화면은 정렬된 것처럼 보이는데 값이 다르다.
     * service(CHAT | COMMERCE | BOTH | NONE)가 있으면 이용 서비스로 거른다. 모르는 값이면 400.
     */
    @Operation(
        summary = "회원 목록 검색",
        description = "이메일·이름으로 회원을 찾아 페이지로 돌려준다(비우면 전체). 기본 정렬은 이름 가나다순, 한글 이름이 먼저이고 이름 없는 회원은 끝. " +
            "각 줄에 직원 권한과 이용 서비스가 붙는다. 모르는 sort·service 값이면 400.",
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
        @Parameter(description = "이용 서비스 필터. CHAT | COMMERCE | BOTH | NONE(이용 기록 없음). 비우면 거르지 않는다.", example = "CHAT")
        @RequestParam(value = "service", required = false) service: ServiceFilter?,
    ): ResponseEntity<Page<AdminMemberSummaryDto>> {
        val memberSort = MemberSort.parse(sort)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 정렬입니다: $sort")
        return ResponseEntity.ok(
            memberAdminUseCase.searchMembers(
                keyword, memberSort,
                PageRequest.of(maxOf(page, 0), minOf(maxOf(size, 1), 100)),
                service,
            ).map(AdminMemberSummaryDto::of),
        )
    }

    /** 백오피스에 로그인한 본인 정보. 게이트웨이가 JWT subject 를 X-Auth-User-Id 헤더로 넣어 준다. */
    @Operation(
        summary = "내 정보 조회",
        description = "어드민에 로그인한 직원 본인의 회원 상세(친구 목록·직원 권한·이용 서비스 포함)를 돌려준다. 헤더가 없으면 401, 없는 회원이면 404.",
    )
    @GetMapping("/me")
    fun me(
        @Parameter(description = "로그인한 직원 userId. 게이트웨이가 JWT sub 로 넣는다.", example = "112233445566778899001")
        @RequestHeader(value = "X-Auth-User-Id", required = false) userId: String?,
    ): ResponseEntity<AdminMemberDetailDto> {
        if (userId.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        return ResponseEntity.ok(AdminMemberDetailDto.of(memberAdminUseCase.getMe(userId)))
    }

    /** 백오피스에서 본인 정보를 수정한다. */
    @Operation(
        summary = "내 정보 수정",
        description = "직원 본인의 이름·상태 메시지·프로필 사진·배경 사진을 고친다. 본문에서 null 인 필드는 기존 값을 유지한다. " +
            "고친 뒤 회원 상세를 돌려준다. 헤더가 없으면 401.",
    )
    @PutMapping("/me")
    fun updateMe(
        @Parameter(description = "로그인한 직원 userId. 게이트웨이가 JWT sub 로 넣는다.", example = "112233445566778899001")
        @RequestHeader(value = "X-Auth-User-Id", required = false) userId: String?,
        @RequestBody request: UpdateProfileDto,
    ): ResponseEntity<AdminMemberDetailDto> {
        if (userId.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        return ResponseEntity.ok(AdminMemberDetailDto.of(memberAdminUseCase.updateMe(userId, request.toCommand())))
    }

    @Operation(
        summary = "회원 상세 조회",
        description = "회원 한 명의 정보와 친구 수·친구 목록(이름순)·직원 권한·서비스별 처음/마지막 이용 시각(UTC)·회원 상태를 돌려준다. " +
            "없는 회원이면 404.",
    )
    @GetMapping("/{id}")
    fun detail(@Parameter(description = "회원 id(member.id)", example = "11") @PathVariable("id") id: Long): ResponseEntity<AdminMemberDetailDto> =
        ResponseEntity.ok(AdminMemberDetailDto.of(memberAdminUseCase.getMemberDetail(id)))

    /**
     * 회원 상세의 친구 탭. filter = ALL(기본) | NORMAL | FAVORITE | HIDDEN | BLOCKED, 모르는 값이면 400.
     * size 기본 10, 최대 50. 순서는 즐겨찾기 먼저, 표시 이름 가나다순(한글 먼저), id. 없는 회원이면 404.
     */
    @Operation(
        summary = "회원 친구 목록 조회",
        description = "회원 상세의 친구 탭. 이 회원이 추가한 친구를 필터·페이지로 돌려주고 상태별 친구 수(counts)도 함께 준다. " +
            "순서는 즐겨찾기 먼저, 표시 이름(별칭, 없으면 친구 이름) 가나다순(한글 먼저), id. 모르는 filter 면 400, 없는 회원이면 404.",
    )
    @GetMapping("/{id}/friends")
    fun friends(
        @Parameter(description = "회원 id(member.id)", example = "11")
        @PathVariable("id") id: Long,
        @Parameter(description = "ALL(기본, 숨김·차단 포함) | NORMAL | FAVORITE | HIDDEN | BLOCKED. 대소문자 무시.", example = "FAVORITE")
        @RequestParam(value = "filter", required = false) filter: String?,
        @Parameter(description = "페이지 번호(0부터). 기본 0, 음수는 0 으로 본다.", example = "0")
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기. 기본 10, 1~50 으로 맞춘다.", example = "10")
        @RequestParam(value = "size", defaultValue = "10") size: Int,
    ): ResponseEntity<AdminFriendPageDto> {
        val friendFilter = AdminFriendFilter.parse(filter)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 필터입니다: $filter")
        return ResponseEntity.ok(
            AdminFriendPageDto.of(
                memberAdminUseCase.getMemberFriends(id, friendFilter, PageRequest.of(maxOf(page, 0), minOf(maxOf(size, 1), 50))),
            ),
        )
    }
}
