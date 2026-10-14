package com.example.memberservice.api.pub

import com.example.memberservice.api.dto.NoticeDto
import com.example.memberservice.application.usecase.NoticeUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 앱 설정 > 공지사항이 부른다. */
@Tag(name = "공지사항 (앱)", description = "모두의 채팅 앱의 설정 > 공지사항이 게이트웨이를 거쳐 부른다. 모두 계정 토큰(aud modu-chat) 필요 — X-Auth-User-Id 가 없으면 403.")
@RestController
@RequestMapping("/api-public/notice")
class NoticePublicController(private val noticeUseCase: NoticeUseCase) {

    @Operation(summary = "공지사항 목록 조회", description = "공지사항 전부를 최신 글(id 큰 순)부터 돌려준다. 페이지 없이 한 번에 준다.")
    @GetMapping
    fun notices(): ResponseEntity<List<NoticeDto>> = ResponseEntity.ok(noticeUseCase.getNotices().map(NoticeDto::of))

    @Operation(summary = "공지사항 상세 조회", description = "공지 한 건을 돌려준다. 없는 id 면 404.")
    @GetMapping("/{id}")
    fun notice(@Parameter(description = "공지 id", example = "3") @PathVariable("id") id: Long): ResponseEntity<NoticeDto> {
        val notice = noticeUseCase.getNotice(id)
        return if (notice == null) ResponseEntity.notFound().build() else ResponseEntity.ok(NoticeDto.of(notice))
    }
}
