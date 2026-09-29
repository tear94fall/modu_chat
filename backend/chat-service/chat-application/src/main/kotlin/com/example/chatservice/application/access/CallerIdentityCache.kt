package com.example.chatservice.application.access

import com.example.chatservice.application.common.exception.CustomException
import com.example.chatservice.application.common.exception.ErrorCode
import com.example.chatservice.application.member.MemberGateway
import java.util.concurrent.ConcurrentHashMap
import java.util.function.LongSupplier
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * userId(모두 계정 subject) → 회원 id(숫자 PK). 본인 확인과 방 멤버 확인은 요청마다 하는데, 그때마다
 * member-service 를 부르면 왕복이 그대로 지연이 된다. 이 대응은 바뀌지 않는 값이라 10분 캐시한다
 * (BlockedIdsCache 와 같은 방식: ConcurrentHashMap + 만료시각).
 *
 * 조회에 실패하면 만료된 값이라도 있으면 그것을 쓴다. 그것도 없으면 본인 확인을 할 수 없으므로
 * 통과시키지 않고 503 으로 답한다.
 */
@Component
class CallerIdentityCache internal constructor(
    private val memberGateway: MemberGateway,
    private val clock: LongSupplier,
) {

    @Autowired
    constructor(memberGateway: MemberGateway) : this(memberGateway, LongSupplier { System.currentTimeMillis() })

    private val log = LoggerFactory.getLogger(CallerIdentityCache::class.java)
    private val entries = ConcurrentHashMap<String, Entry>()

    fun memberIdOf(userId: String): Long {
        val now = clock.asLong
        val cached = entries[userId]
        if (cached != null && cached.expiresAt > now) {
            return cached.memberId
        }

        val memberId = try {
            memberGateway.byUserId(userId).id
        } catch (e: Exception) {
            log.warn("failed to resolve member id for {}", userId, e)
            null
        }
        if (memberId == null) {
            return cached?.memberId ?: throw CustomException(ErrorCode.MEMBER_SERVICE_UNAVAILABLE)
        }

        purgeIfTooLarge(now)
        entries[userId] = Entry(memberId, now + TTL_MILLIS)
        return memberId
    }

    private fun purgeIfTooLarge(now: Long) {
        if (entries.size < MAX_ENTRIES) {
            return
        }
        entries.entries.removeIf { it.value.expiresAt <= now }
    }

    private data class Entry(val memberId: Long, val expiresAt: Long)

    companion object {
        internal const val TTL_MILLIS = 600_000L
        private const val MAX_ENTRIES = 10_000
    }
}
