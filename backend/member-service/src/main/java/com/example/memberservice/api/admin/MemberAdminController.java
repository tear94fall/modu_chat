package com.example.memberservice.api.admin;

import com.example.memberservice.api.admin.dto.AdminMemberDetailDto;
import com.example.memberservice.api.admin.dto.AdminMemberSummaryDto;
import com.example.memberservice.member.dto.UpdateProfileDto;
import com.example.memberservice.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 백오피스가 게이트웨이(ROLE_ADMIN JWT)를 거쳐 부른다. InternalApiFilter 가 토큰을 검사한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-admin/member")
public class MemberAdminController {

    private final MemberService memberService;

    /** 백오피스 목록은 최신 생성 순. 같은 시각이면 id 내림차순. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdDate"), Sort.Order.desc("id"));

    @GetMapping
    public ResponseEntity<Page<AdminMemberSummaryDto>> search(@RequestParam(value = "keyword", required = false) String keyword,
                                                  @RequestParam(value = "page", defaultValue = "0") int page,
                                                  @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(memberService.searchMembers(keyword, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), NEWEST_FIRST)));
    }

    /** 백오피스에 로그인한 본인 정보. 게이트웨이가 JWT subject 를 X-Auth-User-Id 헤더로 넣어 준다. */
    @GetMapping("/me")
    public ResponseEntity<AdminMemberDetailDto> me(@RequestHeader(value = "X-Auth-User-Id", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(memberService.getMemberDetailByUserId(userId));
    }

    /** 백오피스에서 본인 정보를 수정한다. */
    @PutMapping("/me")
    public ResponseEntity<AdminMemberDetailDto> updateMe(@RequestHeader(value = "X-Auth-User-Id", required = false) String userId,
                                                           @RequestBody UpdateProfileDto request) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(memberService.updateMyProfile(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminMemberDetailDto> detail(@PathVariable("id") Long id) {
        return ResponseEntity.ok(memberService.getMemberDetail(id));
    }
}
