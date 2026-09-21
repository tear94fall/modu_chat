package com.example.wsservice.block

import com.example.wsservice.member.client.MemberFeignClient
import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

/**
 * "이 발신자를 차단한 사람들" 집합을 발신자 userId 별로 60초 캐시한다.
 * 메시지 한 통마다 member-service 를 부르지 않기 위한 것이고, 외부 캐시 라이브러리는 쓰지 않는다.
 *
 * 조회가 실패하면 빈 집합을 준다 — 차단이 한 번 새는 것보다 메시지가 유실되는 쪽이 나쁘다.
 * 실패는 캐시하지 않는다(다음 메시지에서 다시 시도한다).
 */
@Component
class BlockRelationCache(private val memberFeignClient: MemberFeignClient, private val clock: Clock) {

    /** 스프링이 쓰는 생성자. 테스트는 시간을 움직이려고 Clock 을 직접 넣는다. */
    @Autowired
    constructor(memberFeignClient: MemberFeignClient) : this(memberFeignClient, Clock.systemUTC())

    private val log = LoggerFactory.getLogger(BlockRelationCache::class.java)
    private val cache = ConcurrentHashMap<String, Entry>()

    fun blockedBy(senderUserId: String?): Set<String> {
        if (senderUserId.isNullOrBlank()) return emptySet()

        val now = clock.millis()
        val cached = cache[senderUserId]
        if (cached != null && now < cached.expiresAt) return cached.blockedBy

        return try {
            val fetched = memberFeignClient.blockedBy(senderUserId)
            val blockedBy: Set<String> = fetched.orEmpty().filter { !it.isNullOrBlank() }.map { it!! }.toSet()
            cache[senderUserId] = Entry(now + TTL_MILLIS, blockedBy)
            blockedBy
        } catch (e: FeignException) {
            log.warn("[block] blocked-by lookup failed for {} (status {}), treating as not blocked", senderUserId, e.status())
            emptySet()
        } catch (e: RuntimeException) {
            log.warn("[block] blocked-by lookup failed for {}, treating as not blocked", senderUserId, e)
            emptySet()
        }
    }

    fun invalidate(senderUserId: String?) {
        if (senderUserId == null) return
        cache.remove(senderUserId)
    }

    fun clear() = cache.clear()

    private data class Entry(val expiresAt: Long, val blockedBy: Set<String>)

    companion object {
        const val TTL_MILLIS = 60_000L
    }
}
