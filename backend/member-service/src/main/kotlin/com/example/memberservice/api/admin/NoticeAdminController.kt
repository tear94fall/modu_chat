package com.example.memberservice.api.admin

import com.example.memberservice.notice.dto.CreateNoticeDto
import com.example.memberservice.notice.dto.NoticeDto
import com.example.memberservice.notice.service.NoticeService
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
@RestController
@RequestMapping("/api-admin/notice")
class NoticeAdminController(private val noticeService: NoticeService) {

    @GetMapping
    fun search(
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "15") size: Int,
    ): ResponseEntity<Page<NoticeDto>> = ResponseEntity.ok(
        noticeService.searchNotices(PageRequest.of(maxOf(page, 0), minOf(maxOf(size, 1), 100))),
    )

    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateNoticeDto,
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
