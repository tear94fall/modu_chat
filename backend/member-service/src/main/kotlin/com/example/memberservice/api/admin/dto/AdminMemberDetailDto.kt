package com.example.memberservice.api.admin.dto

import com.example.memberservice.member.dto.MemberDto
import java.time.LocalDateTime

class AdminMemberDetailDto(
    val member: MemberDto,
    val friendCount: Int,
    val createdDate: LocalDateTime?,
    /** 친구 목록. 회원 조회 화면에서 누구와 친구인지 바로 보이도록 함께 내려준다. */
    val friends: List<AdminMemberSummaryDto>,
)
