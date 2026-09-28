package com.example.memberservice.api.admin

import com.example.memberservice.notice.dto.CreateNoticeDto
import com.example.memberservice.notice.dto.NoticeDto
import com.example.memberservice.notice.service.NoticeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 백오피스가 게이트웨이(ROLE_ADMIN JWT)를 거쳐 부른다. InternalApiFilter 가 토큰을 검사한다. */
@Tag(
    name = "공지사항 관리 (어드민)",
    description = "어드민 콘솔용. 게이트웨이가 직원 토큰(ROLE_ADMIN, aud modu-admin)을 확인하고 X-Internal-Token 을 붙인다.",
)
@RestController
@RequestMapping("/api-admin/notice")
class NoticeAdminController(private val noticeService: NoticeService) {

    @Operation(summary = "공지사항 목록 조회", description = "공지사항을 최신 글(id 큰 순)부터 페이지로 돌려준다.")
    @GetMapping
    fun search(
        @Parameter(description = "페이지 번호(0부터). 기본 0, 음수는 0 으로 본다.", example = "0")
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기. 기본 15, 1~100 으로 맞춘다.", example = "15")
        @RequestParam(value = "size", defaultValue = "15") size: Int,
    ): ResponseEntity<Page<NoticeDto>> = ResponseEntity.ok(
        noticeService.searchNotices(PageRequest.of(maxOf(page, 0), minOf(maxOf(size, 1), 100))),
    )

    @Operation(
        summary = "공지사항 등록",
        description = "공지를 저장하고 push=true(기본)면 전체 회원에게 푸시로도 보낸다. 푸시가 실패해도 공지는 남는다. " +
            "작성자는 X-Auth-User-Id 회원의 이름(없으면 기본 이름)으로 정한다. 제목·내용이 비면 400.",
    )
    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateNoticeDto,
        @Parameter(description = "작성한 직원 userId. 게이트웨이가 JWT sub 로 넣는다.", example = "112233445566778899001")
        @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) writerUserId: String?,
    ): ResponseEntity<NoticeDto> = ResponseEntity.ok(noticeService.createNotice(request, writerUserId))

    companion object {
        /**
         * 게이트웨이가 JWT 를 검증한 뒤 넣는 헤더. 클라이언트가 보낸 같은 이름의 헤더는
         * StripClientIdentityFilter 가 라우팅 전에 지우므로 여기 값은 위조할 수 없다.
         */
        private const val AUTH_USER_ID_HEADER = "X-Auth-User-Id"
    }
}
