package com.example.memberservice.api.pub

import com.example.memberservice.notice.dto.NoticeDto
import com.example.memberservice.notice.service.NoticeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 앱 설정 > 공지사항이 부른다. */
@RestController
@RequestMapping("/api-public/notice")
class NoticePublicController(private val noticeService: NoticeService) {

    @GetMapping
    fun notices(): ResponseEntity<List<NoticeDto>> = ResponseEntity.ok(noticeService.getNotices())

    @GetMapping("/{id}")
    fun notice(@PathVariable("id") id: Long): ResponseEntity<NoticeDto> {
        val notice = noticeService.getNotice(id)
        return if (notice == null) ResponseEntity.notFound().build() else ResponseEntity.ok(notice)
    }
}
