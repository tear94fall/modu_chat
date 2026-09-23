package com.example.pointservice.member

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * member-service 조회를 감싼다. 이름·이메일은 표시용이라, member-service 가 죽어도 포인트 화면이 500 이 되지 않게
 * 실패는 빈 결과로 삼킨다(그때는 이름 없이 잔액만 보인다).
 */
@Component
class MemberLookup(private val memberFeignClient: MemberFeignClient) {

    private val log = LoggerFactory.getLogger(MemberLookup::class.java)

    /** userId → 회원 요약. 없는 사용자는 빠진다. */
    fun byUserIds(userIds: Collection<String>): Map<String, MemberSummaryDto> {
        if (userIds.isEmpty()) return emptyMap()
        return try {
            memberFeignClient.getMembersByUserId(userIds.distinct())
                .filter { it.userId != null }
                .associateBy { it.userId!! }
        } catch (e: Exception) {
            log.warn("member-service 조회 실패 — 이름 없이 답한다: {}", e.message)
            emptyMap()
        }
    }

    /** 이름·이메일·userId 검색에 걸린 회원들. 백오피스 검색은 앞 [MAX_MATCHES] 명까지만 본다. */
    fun search(keyword: String): List<MemberSummaryDto> =
        try {
            memberFeignClient.searchMembers(keyword, 0, MAX_MATCHES).content.filter { it.userId != null }
        } catch (e: Exception) {
            log.warn("member-service 검색 실패 — 빈 결과: {}", e.message)
            emptyList()
        }

    companion object {
        const val MAX_MATCHES = 100
    }
}
