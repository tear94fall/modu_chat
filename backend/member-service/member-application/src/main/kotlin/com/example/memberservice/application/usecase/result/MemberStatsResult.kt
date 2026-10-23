package com.example.memberservice.application.usecase.result

import java.time.LocalDate

/** 백오피스 홈 "오늘의 숫자" 중 회원 부분. [days] 는 오래된 날부터 오늘까지. */
data class MemberStatsResult(
    val zone: String,
    val days: List<MemberStatsDay>,
    /** 탈퇴하지 않은 회원 수. */
    val totalMembers: Long,
    /** 서비스(CHAT·COMMERCE)별 활동 회원 — 마지막 이용 시각이 오늘·최근 7일 안인 회원 수. */
    val active: List<ActiveMembersResult>,
)

data class MemberStatsDay(val date: LocalDate, val signups: Int)

data class ActiveMembersResult(val service: String, val today: Long, val week: Long)
