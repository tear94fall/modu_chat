package com.example.chatservice.member.service

import com.example.chatservice.member.client.MemberFeignClient
import java.util.concurrent.atomic.AtomicLong
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** TTL 과 실패 처리. 시계를 직접 밀어 만료를 확인한다(Thread.sleep 없이). */
class BlockedIdsCacheTest {

    private lateinit var memberFeignClient: MemberFeignClient
    private lateinit var now: AtomicLong
    private lateinit var cache: BlockedIdsCache

    @BeforeEach
    fun setUp() {
        memberFeignClient = mock()
        now = AtomicLong(1_000L)
        cache = BlockedIdsCache(memberFeignClient) { now.get() }
    }

    @Test
    @DisplayName("60초 안에는 캐시를 쓰고 만료되면 다시 조회한다")
    fun cachesForTtlThenRefetches() {
        whenever(memberFeignClient.getBlockedIds("me")).thenReturn(listOf("blocked-1"))

        assertThat(cache.get("me")).containsExactly("blocked-1")
        now.addAndGet(59_000L)
        assertThat(cache.get("me")).containsExactly("blocked-1")
        verify(memberFeignClient, times(1)).getBlockedIds("me")

        whenever(memberFeignClient.getBlockedIds("me")).thenReturn(listOf("blocked-1", "blocked-2"))
        now.addAndGet(2_000L) // 총 61초 → 만료
        assertThat(cache.get("me")).containsExactlyInAnyOrder("blocked-1", "blocked-2")
        verify(memberFeignClient, times(2)).getBlockedIds("me")
    }

    @Test
    @DisplayName("사람마다 따로 캐시한다")
    fun cachesPerUser() {
        whenever(memberFeignClient.getBlockedIds("a")).thenReturn(listOf("x"))
        whenever(memberFeignClient.getBlockedIds("b")).thenReturn(listOf("y"))

        assertThat(cache.get("a")).containsExactly("x")
        assertThat(cache.get("b")).containsExactly("y")
        assertThat(cache.get("a")).containsExactly("x")
        verify(memberFeignClient, times(1)).getBlockedIds("a")
    }

    @Test
    @DisplayName("조회가 실패하면 빈 집합을 주고 실패를 캐시하지 않는다")
    fun failureYieldsEmptySetAndIsNotCached() {
        doThrow(RuntimeException("member-service down")).whenever(memberFeignClient).getBlockedIds("me")

        assertThat(cache.get("me")).isEmpty()
        assertThat(cache.size()).isZero()

        // 예외를 스텁한 목은 whenever(...) 으로 다시 스텁하면 그 자리에서 던진다. doReturn 으로 덮는다.
        doReturn(listOf("blocked-1")).whenever(memberFeignClient).getBlockedIds("me")
        assertThat(cache.get("me")).containsExactly("blocked-1")
    }

    @Test
    @DisplayName("실패해도 이미 캐시된 다른 사람의 값은 살아 있다")
    fun failureDoesNotEvictOtherUsers() {
        whenever(memberFeignClient.getBlockedIds("a")).thenReturn(listOf("x"))
        assertThat(cache.get("a")).containsExactly("x")

        doThrow(RuntimeException("boom")).whenever(memberFeignClient).getBlockedIds("b")
        assertThat(cache.get("b")).isEmpty()

        assertThat(cache.get("a")).containsExactly("x")
        verify(memberFeignClient, times(1)).getBlockedIds("a")
    }

    @Test
    @DisplayName("userId 가 없으면(헤더 없음) 조회하지 않고 빈 집합이다")
    fun noUserIdMeansNoLookup() {
        assertThat(cache.get(null)).isEqualTo(setOf<String>())
        assertThat(cache.get("  ")).isEqualTo(setOf<String>())
        verify(memberFeignClient, never()).getBlockedIds(any())
    }

    @Test
    @DisplayName("응답이 null 이어도 빈 집합으로 다룬다")
    fun nullResponseIsEmpty() {
        whenever(memberFeignClient.getBlockedIds("me")).thenReturn(null)
        assertThat(cache.get("me")).isEmpty()
    }
}
