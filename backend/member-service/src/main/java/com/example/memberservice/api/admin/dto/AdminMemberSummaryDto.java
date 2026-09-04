package com.example.memberservice.api.admin.dto;

import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/** 백오피스 회원 목록에 노출할 요약 정보. */
@Getter
@AllArgsConstructor
public class AdminMemberSummaryDto {
    private Long id;
    private String userId;
    private String username;
    private String email;
    private Role role;
    private LocalDateTime createdDate;

    public static AdminMemberSummaryDto from(Member member) {
        return new AdminMemberSummaryDto(
                member.getId(),
                member.getUserId(),
                member.getUsername(),
                member.getEmail(),
                member.getRole(),
                member.getCreatedDate()
        );
    }
}
