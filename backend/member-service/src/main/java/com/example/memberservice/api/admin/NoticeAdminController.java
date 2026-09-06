package com.example.memberservice.api.admin;

import com.example.memberservice.notice.dto.CreateNoticeDto;
import com.example.memberservice.notice.dto.NoticeDto;
import com.example.memberservice.notice.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 백오피스가 게이트웨이(ROLE_ADMIN JWT)를 거쳐 부른다. InternalApiFilter 가 토큰을 검사한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-admin/notice")
public class NoticeAdminController {

    /**
     * 게이트웨이가 JWT 를 검증한 뒤 넣는 헤더. 클라이언트가 보낸 같은 이름의 헤더는
     * StripClientIdentityFilter 가 라우팅 전에 지우므로 여기 값은 위조할 수 없다.
     */
    private static final String AUTH_USER_ID_HEADER = "X-Auth-User-Id";

    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<Page<NoticeDto>> search(@RequestParam(value = "page", defaultValue = "0") int page,
                                                  @RequestParam(value = "size", defaultValue = "15") int size) {
        return ResponseEntity.ok(noticeService.searchNotices(
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))));
    }

    @PostMapping
    public ResponseEntity<NoticeDto> create(@Valid @RequestBody CreateNoticeDto request,
                                            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String writerUserId) {
        return ResponseEntity.ok(noticeService.createNotice(request, writerUserId));
    }
}
