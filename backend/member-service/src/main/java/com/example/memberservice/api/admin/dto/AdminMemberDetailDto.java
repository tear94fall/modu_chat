package com.example.memberservice.api.admin.dto;

import com.example.memberservice.member.dto.MemberDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class AdminMemberDetailDto {
    private MemberDto member;
    private int friendCount;
    private LocalDateTime createdDate;
    /** 친구 목록. 회원 조회 화면에서 누구와 친구인지 바로 보이도록 함께 내려준다. */
    private List<AdminMemberSummaryDto> friends;
}
