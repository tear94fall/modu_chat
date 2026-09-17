package com.example.memberservice.api.pub;

import com.example.memberservice.notice.dto.NoticeDto;
import com.example.memberservice.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 앱 설정 > 공지사항이 부른다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-public/notice")
public class NoticePublicController {

    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<List<NoticeDto>> notices() {
        return ResponseEntity.ok(noticeService.getNotices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoticeDto> notice(@PathVariable("id") Long id) {
        NoticeDto notice = noticeService.getNotice(id);
        return notice == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(notice);
    }
}
