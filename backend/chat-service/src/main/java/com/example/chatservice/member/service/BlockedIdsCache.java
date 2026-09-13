package com.example.chatservice.member.service;

import com.example.chatservice.member.client.MemberFeignClient;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
@Slf4j
@Component
public class BlockedIdsCache {

    static final long TTL_MILLIS = 60_000L;

    /** 캐시가 무한히 자라지 않게 하는 상한. 넘으면 만료된 항목부터 비운다. */
    private static final int MAX_ENTRIES = 10_000;

    private final MemberFeignClient memberFeignClient;
    private final LongSupplier clock;
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    @Autowired
    public BlockedIdsCache(MemberFeignClient memberFeignClient) {
        this(memberFeignClient, System::currentTimeMillis);
    }

    /** 테스트가 시간을 밀어 만료를 확인할 수 있게 시계를 주입받는 생성자. */
    BlockedIdsCache(MemberFeignClient memberFeignClient, LongSupplier clock) {
        this.memberFeignClient = memberFeignClient;
        this.clock = clock;
    }

    /** userId 가 차단한 사람들. 헤더가 없어 userId 가 null 이면 필터 없음(빈 집합). */
    public Set<String> get(String userId) {
        if (userId == null || userId.isBlank()) {
            return Set.of();
        }

        long now = clock.getAsLong();
        Entry cached = entries.get(userId);
        if (cached != null && cached.expiresAt > now) {
            return cached.ids;
        }

        Set<String> ids;
        try {
            List<String> fetched = memberFeignClient.getBlockedIds(userId);
            ids = fetched == null ? Set.of() : fetched.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (Exception e) {
            log.warn("failed to load blocked ids for {}, treating as no block", userId, e);
            entries.remove(userId);
            return Set.of();
        }

        purgeIfTooLarge(now);
        entries.put(userId, new Entry(ids, now + TTL_MILLIS));
        return ids;
    }

    private void purgeIfTooLarge(long now) {
        if (entries.size() < MAX_ENTRIES) {
            return;
        }
        entries.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
    }

    /** 테스트용. */
    int size() {
        return entries.size();
    }

    private record Entry(Set<String> ids, long expiresAt) {
    }
}
