package com.example.chatservice.application.access

import com.example.chatservice.application.common.exception.CustomException
import com.example.chatservice.application.common.exception.ErrorCode
import com.example.chatservice.application.member.MemberGateway
import com.example.chatservice.application.member.MemberInfo
import java.util.concurrent.atomic.AtomicLong
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** userId → 회원 id 캐시. 시계를 직접 밀어 만료를 확인한다. */
class CallerIdentityCacheTest {

    private lateinit var memberGateway: MemberGateway
    private lateinit var now: AtomicLong
    private lateinit var cache: CallerIdentityCache

    @BeforeEach
    fun setUp() {
        memberGateway = mock()
        now = AtomicLong(1_000L)
        cache = CallerIdentityCache(memberGateway) { now.get() }
    }

    @Test
    @DisplayName("10분 안에는 member-service 를 다시 부르지 않는다")
    fun cachesForTtl() {
        whenever(memberGateway.byUserId("me")).thenReturn(MemberInfo(id = 7L, userId = "me"))

        assertThat(cache.memberIdOf("me")).isEqualTo(7L)
        now.addAndGet(CallerIdentityCache.TTL_MILLIS - 1)
        assertThat(cache.memberIdOf("me")).isEqualTo(7L)
        verify(memberGateway, times(1)).byUserId("me")

        now.addAndGet(2)
        assertThat(cache.memberIdOf("me")).isEqualTo(7L)
        verify(memberGateway, times(2)).byUserId("me")
    }

    @Test
    @DisplayName("조회에 실패해도 만료된 값이 있으면 그것으로 본인 확인을 이어 간다")
    fun staleValueSurvivesFailure() {
        whenever(memberGateway.byUserId("me")).thenReturn(MemberInfo(id = 7L, userId = "me"))
        assertThat(cache.memberIdOf("me")).isEqualTo(7L)

        now.addAndGet(CallerIdentityCache.TTL_MILLIS + 1)
        doThrow(RuntimeException("member-service down")).whenever(memberGateway).byUserId("me")

        assertThat(cache.memberIdOf("me")).isEqualTo(7L)
    }

    @Test
    @DisplayName("누구인지 알 수 없으면 통과시키지 않고 503 이다. 실패는 캐시하지 않는다")
    fun unknownIdentityIsRejected() {
        doThrow(RuntimeException("member-service down")).whenever(memberGateway).byUserId("me")

        assertThatThrownBy { cache.memberIdOf("me") }
            .isInstanceOfSatisfying(CustomException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.MEMBER_SERVICE_UNAVAILABLE)
            }

        doReturn(MemberInfo(id = 7L, userId = "me")).whenever(memberGateway).byUserId("me")
        assertThat(cache.memberIdOf("me")).isEqualTo(7L)
    }
}
