package com.example.pointservice.api.member

import com.example.pointservice.application.member.MemberLookup
import com.example.pointservice.application.member.MemberSummary
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * [MemberLookup] 의 Feign 구현. 이름·이메일은 표시용이라, member-service 가 죽어도 포인트 화면이 500 이 되지 않게
 * 실패는 빈 결과로 삼킨다(그때는 이름 없이 잔액만 보인다).
 */
@Component
class FeignMemberLookup(private val memberFeignClient: MemberFeignClient) : MemberLookup {

    private val log = LoggerFactory.getLogger(FeignMemberLookup::class.java)

    override fun byUserIds(userIds: Collection<String>): Map<String, MemberSummary> {
        if (userIds.isEmpty()) return emptyMap()
        return try {
            memberFeignClient.getMembersByUserId(userIds.distinct()).mapNotNull { it.toSummary() }.associateBy { it.userId }
        } catch (e: Exception) {
            log.warn("member-service 조회 실패 — 이름 없이 답한다: {}", e.message)
            emptyMap()
        }
    }

    /** 백오피스 검색은 앞 [MAX_MATCHES] 명까지만 본다. */
    override fun search(keyword: String): List<MemberSummary> =
        try {
            memberFeignClient.searchMembers(keyword, 0, MAX_MATCHES).content.mapNotNull { it.toSummary() }
        } catch (e: Exception) {
            log.warn("member-service 검색 실패 — 빈 결과: {}", e.message)
            emptyList()
        }

    private fun MemberSummaryDto.toSummary(): MemberSummary? = userId?.let { MemberSummary(it, username, email) }

    companion object {
        const val MAX_MATCHES = 100
    }
}
