package com.example.wsservice.block

import com.example.wsservice.member.client.MemberFeignClient
import feign.FeignException
import feign.Request
import feign.RequestTemplate
import feign.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class BlockRelationCacheTest {

    private lateinit var memberFeignClient: MemberFeignClient
    private lateinit var clock: MutableClock
    private lateinit var cache: BlockRelationCache

    @BeforeEach
    fun setUp() {
        memberFeignClient = mock()
        clock = MutableClock(Instant.parse("2026-09-13T00:00:00Z"))
        cache = BlockRelationCache(memberFeignClient, clock)
    }

    @Test
    @DisplayName("TTL 안에서는 member-service 를 다시 부르지 않는다")
    fun withinTtl_servesFromCache() {
        whenever(memberFeignClient.blockedBy("user-a")).thenReturn(listOf("user-b"))

        assertThat(cache.blockedBy("user-a")).containsExactly("user-b")
        clock.advance(BlockRelationCache.TTL_MILLIS - 1)
        assertThat(cache.blockedBy("user-a")).containsExactly("user-b")

        verify(memberFeignClient, times(1)).blockedBy("user-a")
    }

    @Test
    @DisplayName("TTL 이 지나면 다시 조회한다")
    fun afterTtl_refetches() {
        whenever(memberFeignClient.blockedBy("user-a")).thenReturn(listOf("user-b"), listOf())

        assertThat(cache.blockedBy("user-a")).containsExactly("user-b")
        clock.advance(BlockRelationCache.TTL_MILLIS)
        assertThat(cache.blockedBy("user-a")).isEmpty()

        verify(memberFeignClient, times(2)).blockedBy("user-a")
    }

    @Test
    @DisplayName("발신자마다 따로 캐시한다")
    fun cachesPerSender() {
        whenever(memberFeignClient.blockedBy("user-a")).thenReturn(listOf("user-b"))
        whenever(memberFeignClient.blockedBy("user-c")).thenReturn(listOf())

        assertThat(cache.blockedBy("user-a")).containsExactly("user-b")
        assertThat(cache.blockedBy("user-c")).isEmpty()
        assertThat(cache.blockedBy("user-a")).containsExactly("user-b")

        verify(memberFeignClient, times(1)).blockedBy("user-a")
        verify(memberFeignClient, times(1)).blockedBy("user-c")
    }

    @Test
    @DisplayName("빈 결과도 캐시한다")
    fun cachesEmptyResult() {
        whenever(memberFeignClient.blockedBy("user-a")).thenReturn(listOf())

        assertThat(cache.blockedBy("user-a")).isEmpty()
        assertThat(cache.blockedBy("user-a")).isEmpty()

        verify(memberFeignClient, times(1)).blockedBy("user-a")
    }

    @Test
    @DisplayName("member-service 가 5xx 면 빈 집합으로 취급한다")
    fun feignException_returnsEmptySet() {
        whenever(memberFeignClient.blockedBy("user-a")).thenThrow(serverError())

        assertThat(cache.blockedBy("user-a")).isEmpty()
    }

    @Test
    @DisplayName("조회 실패는 캐시하지 않아 다음 메시지에서 다시 시도한다")
    fun failure_isNotCached() {
        whenever(memberFeignClient.blockedBy("user-a")).thenThrow(RuntimeException("timeout")).thenReturn(listOf("user-b"))

        assertThat(cache.blockedBy("user-a")).isEmpty()
        assertThat(cache.blockedBy("user-a")).containsExactly("user-b")

        verify(memberFeignClient, times(2)).blockedBy("user-a")
    }

    @Test
    @DisplayName("응답이 null 이거나 빈 userId 가 섞여 있어도 안전하다")
    fun toleratesNullResponseAndBlankEntries() {
        whenever(memberFeignClient.blockedBy("user-a")).thenReturn(null)
        whenever(memberFeignClient.blockedBy("user-c")).thenReturn(listOf("user-b", null, " "))

        assertThat(cache.blockedBy("user-a")).isEmpty()
        assertThat(cache.blockedBy("user-c")).containsExactly("user-b")
    }

    @Test
    @DisplayName("sender 가 없으면 조회하지 않는다")
    fun blankSender_skipsLookup() {
        assertThat(cache.blockedBy(null)).isEmpty()
        assertThat(cache.blockedBy(" ")).isEmpty()

        verify(memberFeignClient, times(0)).blockedBy(anyOrNull())
    }

    @Test
    @DisplayName("invalidate/clear 는 캐시를 비운다")
    fun invalidateAndClear() {
        whenever(memberFeignClient.blockedBy("user-a")).thenReturn(listOf("user-b"))

        cache.blockedBy("user-a")
        cache.invalidate("user-a")
        cache.blockedBy("user-a")
        cache.clear()
        cache.blockedBy("user-a")

        verify(memberFeignClient, times(3)).blockedBy("user-a")
    }

    private fun serverError(): FeignException {
        val request = Request.create(Request.HttpMethod.GET, "/api-internal/member/user-a/blocked-by", mapOf(), ByteArray(0), StandardCharsets.UTF_8, RequestTemplate())
        return FeignException.errorStatus("MemberFeignClient#blockedBy(String)", Response.builder().status(500).reason("boom").request(request).headers(mapOf()).build())
    }

    /** 테스트에서 시간을 직접 움직이는 Clock. */
    private class MutableClock(private var instant: Instant) : Clock() {
        fun advance(millis: Long) {
            instant = instant.plusMillis(millis)
        }

        override fun getZone(): ZoneId = ZoneId.of("UTC")
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = instant
    }
}
