package com.example.chatservice.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.chatservice.member.client.MemberFeignClient;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TTL 과 실패 처리. 시계를 직접 밀어 만료를 확인한다(Thread.sleep 없이). */
class BlockedIdsCacheTest {

    private MemberFeignClient memberFeignClient;
    private AtomicLong now;
    private BlockedIdsCache cache;

    @BeforeEach
    void setUp() {
        memberFeignClient = mock(MemberFeignClient.class);
        now = new AtomicLong(1_000L);
        cache = new BlockedIdsCache(memberFeignClient, now::get);
    }

    @Test
    @DisplayName("60초 안에는 캐시를 쓰고 만료되면 다시 조회한다")
    void cachesForTtlThenRefetches() {
        when(memberFeignClient.getBlockedIds("me")).thenReturn(List.of("blocked-1"));

        assertThat(cache.get("me")).containsExactly("blocked-1");
        now.addAndGet(59_000L);
        assertThat(cache.get("me")).containsExactly("blocked-1");
        verify(memberFeignClient, times(1)).getBlockedIds("me");

        when(memberFeignClient.getBlockedIds("me")).thenReturn(List.of("blocked-1", "blocked-2"));
        now.addAndGet(2_000L); // 총 61초 → 만료
        assertThat(cache.get("me")).containsExactlyInAnyOrder("blocked-1", "blocked-2");
        verify(memberFeignClient, times(2)).getBlockedIds("me");
    }

    @Test
    @DisplayName("사람마다 따로 캐시한다")
    void cachesPerUser() {
        when(memberFeignClient.getBlockedIds("a")).thenReturn(List.of("x"));
        when(memberFeignClient.getBlockedIds("b")).thenReturn(List.of("y"));

        assertThat(cache.get("a")).containsExactly("x");
        assertThat(cache.get("b")).containsExactly("y");
        assertThat(cache.get("a")).containsExactly("x");
        verify(memberFeignClient, times(1)).getBlockedIds("a");
    }

    @Test
    @DisplayName("조회가 실패하면 빈 집합을 주고 실패를 캐시하지 않는다")
    void failureYieldsEmptySetAndIsNotCached() {
        doThrow(new RuntimeException("member-service down")).when(memberFeignClient).getBlockedIds("me");

        assertThat(cache.get("me")).isEmpty();
        assertThat(cache.size()).isZero();

        // 예외를 스텁한 목은 when(...) 으로 다시 스텁하면 그 자리에서 던진다. doReturn 으로 덮는다.
        doReturn(List.of("blocked-1")).when(memberFeignClient).getBlockedIds("me");
        assertThat(cache.get("me")).containsExactly("blocked-1");
    }

    @Test
    @DisplayName("실패해도 이미 캐시된 다른 사람의 값은 살아 있다")
    void failureDoesNotEvictOtherUsers() {
        when(memberFeignClient.getBlockedIds("a")).thenReturn(List.of("x"));
        assertThat(cache.get("a")).containsExactly("x");

        doThrow(new RuntimeException("boom")).when(memberFeignClient).getBlockedIds("b");
        assertThat(cache.get("b")).isEmpty();

        assertThat(cache.get("a")).containsExactly("x");
        verify(memberFeignClient, times(1)).getBlockedIds("a");
    }

    @Test
    @DisplayName("userId 가 없으면(헤더 없음) 조회하지 않고 빈 집합이다")
    void noUserIdMeansNoLookup() {
        assertThat(cache.get(null)).isEqualTo(Set.of());
        assertThat(cache.get("  ")).isEqualTo(Set.of());
        verify(memberFeignClient, never()).getBlockedIds(anyString());
    }

    @Test
    @DisplayName("응답이 null 이어도 빈 집합으로 다룬다")
    void nullResponseIsEmpty() {
        when(memberFeignClient.getBlockedIds("me")).thenReturn(null);
        assertThat(cache.get("me")).isEmpty();
    }
}
