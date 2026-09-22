package com.example.chatservice.member.service

import com.example.chatservice.member.client.MemberFeignClient
import java.util.concurrent.ConcurrentHashMap
import java.util.function.LongSupplier
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * "이 사람이 차단한 userId 들" 을 60초 캐시한다. 이력 조회는 방을 열 때마다 여러 번 오는데
 * 그때마다 member-service 를 부르면 왕복이 그대로 지연이 된다.
 *
 * 외부 캐시 라이브러리 없이 ConcurrentHashMap + 만료시각으로 둔다(ws-service 의
 * BlockRelationCache 와 같은 방식).
 *
 * 조회 실패(5xx/타임아웃)는 빈 집합으로 본다 — 차단이 한 번 새는 것이 방이 통째로
 * 500 이 되는 것보다 낫다. 실패는 캐시하지 않아서 member-service 가 살아나면
 * 다음 요청부터 바로 반영된다.
 */
@Component
class BlockedIdsCache internal constructor(
    private val memberFeignClient: MemberFeignClient,
    /** 테스트가 시간을 밀어 만료를 확인할 수 있게 시계를 주입받는다. */
    private val clock: LongSupplier,
) {

    @Autowired
    constructor(memberFeignClient: MemberFeignClient) : this(memberFeignClient, LongSupplier { System.currentTimeMillis() })

    private val log = LoggerFactory.getLogger(BlockedIdsCache::class.java)
    private val entries = ConcurrentHashMap<String, Entry>()

    companion object {
        internal const val TTL_MILLIS = 60_000L

        /** 캐시가 무한히 자라지 않게 하는 상한. 넘으면 만료된 항목부터 비운다. */
        private const val MAX_ENTRIES = 10_000
    }

    /** userId 가 차단한 사람들. 헤더가 없어 userId 가 null 이면 필터 없음(빈 집합). */
    fun get(userId: String?): Set<String> {
        if (userId.isNullOrBlank()) {
            return emptySet()
        }

        val now = clock.asLong
        val cached = entries[userId]
        if (cached != null && cached.expiresAt > now) {
            return cached.ids
        }

        val ids: Set<String> = try {
            val fetched = memberFeignClient.getBlockedIds(userId)
            fetched?.filterNotNull()?.toSet() ?: emptySet()
        } catch (e: Exception) {
            log.warn("failed to load blocked ids for {}, treating as no block", userId, e)
            entries.remove(userId)
            return emptySet()
        }

        purgeIfTooLarge(now)
        entries[userId] = Entry(ids, now + TTL_MILLIS)
        return ids
    }

    private fun purgeIfTooLarge(now: Long) {
        if (entries.size < MAX_ENTRIES) {
            return
        }
        entries.entries.removeIf { it.value.expiresAt <= now }
    }

    /** 테스트용. */
    internal fun size(): Int = entries.size

    private data class Entry(val ids: Set<String>, val expiresAt: Long)
}
