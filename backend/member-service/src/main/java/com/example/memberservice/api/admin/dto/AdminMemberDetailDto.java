package com.example.memberservice.api.admin.dto;

import com.example.memberservice.member.dto.MemberDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminMemberDetailDto {
    private MemberDto member;
    private int friendCount;
    private LocalDateTime createdDate;
}
