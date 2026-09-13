package com.example.wsservice.block;

import com.example.wsservice.member.client.MemberFeignClient;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "이 발신자를 차단한 사람들" 집합을 발신자 userId 별로 60초 캐시한다.
 * 메시지 한 통마다 member-service 를 부르지 않기 위한 것이고, 외부 캐시 라이브러리는 쓰지 않는다.
 *
 * 조회가 실패하면 빈 집합을 준다 — 차단이 한 번 새는 것보다 메시지가 유실되는 쪽이 나쁘다.
 * 실패는 캐시하지 않는다(다음 메시지에서 다시 시도한다).
 */
@Slf4j
@Component
public class BlockRelationCache {

    static final long TTL_MILLIS = 60_000L;

    private final MemberFeignClient memberFeignClient;
    private final Clock clock;
    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();

    @Autowired
    public BlockRelationCache(MemberFeignClient memberFeignClient) {
        this(memberFeignClient, Clock.systemUTC());
    }

    /** 테스트에서 시간을 직접 움직이기 위한 생성자. */
    public BlockRelationCache(MemberFeignClient memberFeignClient, Clock clock) {
        this.memberFeignClient = memberFeignClient;
        this.clock = clock;
    }

    public Set<String> blockedBy(String senderUserId) {
        if (senderUserId == null || senderUserId.isBlank()) return Set.of();

        long now = clock.millis();

        Entry cached = cache.get(senderUserId);
        if (cached != null && now < cached.expiresAt()) {
            return cached.blockedBy();
        }

        try {
            List<String> fetched = memberFeignClient.blockedBy(senderUserId);

            Set<String> blockedBy = new HashSet<>();
            if (fetched != null) {
                fetched.stream().filter(userId -> userId != null && !userId.isBlank()).forEach(blockedBy::add);
            }

            Set<String> immutable = Set.copyOf(blockedBy);
            cache.put(senderUserId, new Entry(now + TTL_MILLIS, immutable));

            return immutable;
        } catch (FeignException e) {
            log.warn("[block] blocked-by lookup failed for {} (status {}), treating as not blocked", senderUserId, e.status());
            return Set.of();
        } catch (RuntimeException e) {
            log.warn("[block] blocked-by lookup failed for {}, treating as not blocked", senderUserId, e);
            return Set.of();
        }
    }

    public void invalidate(String senderUserId) {
        if (senderUserId == null) return;
        cache.remove(senderUserId);
    }

    public void clear() {
        cache.clear();
    }

    private record Entry(long expiresAt, Set<String> blockedBy) {
    }
}
