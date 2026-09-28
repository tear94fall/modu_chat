package com.example.memberservice.usage

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * 기동이 끝나면 채팅 이용 기록을 한 번 채운다(멱등). 실패해도 서비스는 계속 뜬다 — 다음 기동 때 다시 한다.
 * 인스턴스 여럿이 동시에 돌면 유일 제약에 걸린 쪽이 실패 로그만 남긴다.
 */
@Component
class UsageBackfillRunner(private val usageService: MemberServiceUsageService) {

    private val log = LoggerFactory.getLogger(UsageBackfillRunner::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun backfill() {
        try {
            usageService.backfillChat()
        } catch (e: Exception) {
            log.warn("채팅 이용 기록 백필 실패, 다음 기동 때 다시 한다 message={}", e.message)
        }
    }
}
