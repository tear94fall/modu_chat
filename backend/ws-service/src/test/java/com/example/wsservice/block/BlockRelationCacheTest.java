package com.example.wsservice.block;

import com.example.wsservice.member.client.MemberFeignClient;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlockRelationCacheTest {

    private MemberFeignClient memberFeignClient;
    private MutableClock clock;
    private BlockRelationCache cache;

    @BeforeEach
    void setUp() {
        memberFeignClient = mock(MemberFeignClient.class);
        clock = new MutableClock(Instant.parse("2026-09-13T00:00:00Z"));
        cache = new BlockRelationCache(memberFeignClient, clock);
    }

    @Test
    @DisplayName("TTL 안에서는 member-service 를 다시 부르지 않는다")
    void withinTtl_servesFromCache() {
        when(memberFeignClient.blockedBy("user-a")).thenReturn(List.of("user-b"));

        assertThat(cache.blockedBy("user-a")).containsExactly("user-b");
        clock.advance(BlockRelationCache.TTL_MILLIS - 1);
        assertThat(cache.blockedBy("user-a")).containsExactly("user-b");

        verify(memberFeignClient, times(1)).blockedBy("user-a");
    }

    @Test
    @DisplayName("TTL 이 지나면 다시 조회한다")
    void afterTtl_refetches() {
        when(memberFeignClient.blockedBy("user-a")).thenReturn(List.of("user-b"), List.of());

        assertThat(cache.blockedBy("user-a")).containsExactly("user-b");
        clock.advance(BlockRelationCache.TTL_MILLIS);
        assertThat(cache.blockedBy("user-a")).isEmpty();

        verify(memberFeignClient, times(2)).blockedBy("user-a");
    }

    @Test
    @DisplayName("발신자마다 따로 캐시한다")
    void cachesPerSender() {
        when(memberFeignClient.blockedBy("user-a")).thenReturn(List.of("user-b"));
        when(memberFeignClient.blockedBy("user-c")).thenReturn(List.of());

        assertThat(cache.blockedBy("user-a")).containsExactly("user-b");
        assertThat(cache.blockedBy("user-c")).isEmpty();
        assertThat(cache.blockedBy("user-a")).containsExactly("user-b");

        verify(memberFeignClient, times(1)).blockedBy("user-a");
        verify(memberFeignClient, times(1)).blockedBy("user-c");
    }

    @Test
    @DisplayName("빈 결과도 캐시한다")
    void cachesEmptyResult() {
        when(memberFeignClient.blockedBy("user-a")).thenReturn(List.of());

        assertThat(cache.blockedBy("user-a")).isEmpty();
        assertThat(cache.blockedBy("user-a")).isEmpty();

        verify(memberFeignClient, times(1)).blockedBy("user-a");
    }

    @Test
    @DisplayName("member-service 가 5xx 면 빈 집합으로 취급한다")
    void feignException_returnsEmptySet() {
        when(memberFeignClient.blockedBy("user-a")).thenThrow(serverError());

        assertThat(cache.blockedBy("user-a")).isEmpty();
    }

    @Test
    @DisplayName("조회 실패는 캐시하지 않아 다음 메시지에서 다시 시도한다")
    void failure_isNotCached() {
        when(memberFeignClient.blockedBy("user-a"))
                .thenThrow(new RuntimeException("timeout"))
                .thenReturn(List.of("user-b"));

        assertThat(cache.blockedBy("user-a")).isEmpty();
        assertThat(cache.blockedBy("user-a")).containsExactly("user-b");

        verify(memberFeignClient, times(2)).blockedBy("user-a");
    }

    @Test
    @DisplayName("응답이 null 이거나 빈 userId 가 섞여 있어도 안전하다")
    void toleratesNullResponseAndBlankEntries() {
        when(memberFeignClient.blockedBy("user-a")).thenReturn(null);
        when(memberFeignClient.blockedBy("user-c")).thenReturn(java.util.Arrays.asList("user-b", null, " "));

        assertThat(cache.blockedBy("user-a")).isEmpty();
        assertThat(cache.blockedBy("user-c")).containsExactly("user-b");
    }

    @Test
    @DisplayName("sender 가 없으면 조회하지 않는다")
    void blankSender_skipsLookup() {
        assertThat(cache.blockedBy(null)).isEmpty();
        assertThat(cache.blockedBy(" ")).isEmpty();

        verify(memberFeignClient, times(0)).blockedBy(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("invalidate/clear 는 캐시를 비운다")
    void invalidateAndClear() {
        when(memberFeignClient.blockedBy("user-a")).thenReturn(List.of("user-b"));

        cache.blockedBy("user-a");
        cache.invalidate("user-a");
        cache.blockedBy("user-a");
        cache.clear();
        cache.blockedBy("user-a");

        verify(memberFeignClient, times(3)).blockedBy("user-a");
    }

    private FeignException serverError() {
        Request request = Request.create(Request.HttpMethod.GET, "/api-internal/member/user-a/blocked-by",
                Map.of(), new byte[0], StandardCharsets.UTF_8, new RequestTemplate());
        return FeignException.errorStatus("MemberFeignClient#blockedBy(String)",
                feign.Response.builder().status(500).reason("boom").request(request).headers(Map.of()).build());
    }

    /** 테스트에서 시간을 직접 움직이는 Clock. */
    private static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(long millis) {
            instant = instant.plusMillis(millis);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
