package com.example.pointservice.application.member

/** member-service 가 아는 회원 요약. 포인트 계정은 userId 만 알므로 이름·이메일은 여기서 받아 붙인다. */
data class MemberSummary(val userId: String, val username: String?, val email: String?)

/**
 * 회원 조회 포트. 구현(point-api 의 Feign 어댑터)은 표시용 조회라 실패를 빈 결과로 삼킨다 —
 * member-service 가 죽어도 포인트 화면은 이름 없이 잔액만 보여 준다.
 */
interface MemberLookup {

    /** userId → 회원 요약. 없는 사용자는 빠진다. */
    fun byUserIds(userIds: Collection<String>): Map<String, MemberSummary>

    /** 이름·이메일·userId 검색에 걸린 회원들. */
    fun search(keyword: String): List<MemberSummary>
}
