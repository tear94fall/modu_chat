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

/**
 * 어드민 회원 목록 위의 요약 숫자. 이용 서비스 숫자는 탈퇴하지 않은 회원만 센다([both] 는 둘 다, [none] 은 이용 기록이 없는 회원 —
 * 목록의 서비스 필터와 같은 뜻). 가입자 합계는 [active] + [withdrawn].
 */
data class MemberSummaryResult(
    val active: Long,
    val withdrawn: Long,
    val staff: Long,
    val chat: Long,
    val commerce: Long,
    val both: Long,
    val none: Long,
)
