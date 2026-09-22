package com.example.memberservice.aop

import com.example.memberservice.global.lock.ApiLock
import com.example.memberservice.global.lock.ApiLockAcquisitionException
import com.example.memberservice.global.lock.LockParam
import com.example.memberservice.global.lock.Lockable
import java.util.concurrent.TimeUnit
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.redisson.client.RedisConnectionException
import org.redisson.client.RedisTimeoutException

/**
 * ApiLockAop 의 키 조합 로직과 락 획득 성공/실패 흐름을 검증한다.
 * MethodSignature 를 완전히 흉내내기 까다로운 부분(파라미터 애노테이션 조회)이 있어,
 * 키 조합은 모듈 내부의 `resolveKey` 를 직접 호출해 검증하고,
 * tryLock 성공/실패에 따른 흐름은 joinPoint/signature 를 모킹한 종단 테스트로 검증한다.
 */
@ExtendWith(MockitoExtension::class)
class ApiLockAopTest {

    @Mock lateinit var redissonClient: RedissonClient
    @Mock lateinit var aopForTransaction: AopForTransaction
    @Mock lateinit var rLock: RLock
    @Mock lateinit var joinPoint: ProceedingJoinPoint
    @Mock lateinit var methodSignature: MethodSignature

    private lateinit var apiLockAop: ApiLockAop

    @BeforeEach
    fun setUp() {
        apiLockAop = ApiLockAop(redissonClient, aopForTransaction)
    }

    @Test
    fun resolveKey_fromLockableArgument() {
        val method = Fixture::class.java.getDeclaredMethod("withLockable", Fixture.MyLockable::class.java)

        val key = apiLockAop.resolveKey(method, arrayOf(Fixture.MyLockable("abc")), "")

        assertThat(key).isEqualTo("LOCK:abc")
    }

    @Test
    fun resolveKey_fromStringArgument() {
        val method = Fixture::class.java.getDeclaredMethod("withString", String::class.java)

        val key = apiLockAop.resolveKey(method, arrayOf("hello"), "")

        assertThat(key).isEqualTo("LOCK:hello")
    }

    @Test
    fun resolveKey_withPrefix() {
        val method = Fixture::class.java.getDeclaredMethod("withString", String::class.java)

        val key = apiLockAop.resolveKey(method, arrayOf("hello"), "x")

        assertThat(key).isEqualTo("LOCK:x:hello")
    }

    @Test
    fun resolveKey_joinsMultipleLockParamsWithColon() {
        val method = Fixture::class.java.getDeclaredMethod("withTwoParams", String::class.java, String::class.java)

        val key = apiLockAop.resolveKey(method, arrayOf("a", "b"), "")

        assertThat(key).isEqualTo("LOCK:a:b")
    }

    private fun stubJoinPoint(methodName: String) {
        val method = Fixture::class.java.getDeclaredMethod(methodName, String::class.java)
        whenever(joinPoint.signature).thenReturn(methodSignature)
        whenever(methodSignature.method).thenReturn(method)
        whenever(joinPoint.args).thenReturn(arrayOf("hello"))
    }

    @Test
    fun lock_throwsApiLockAcquisitionException_whenTryLockFails_andNeverUnlocks() {
        stubJoinPoint("withString")
        whenever(redissonClient.getLock("LOCK:hello")).thenReturn(rLock)
        whenever(rLock.tryLock(anyLong(), any<TimeUnit>())).thenReturn(false)

        assertThatThrownBy { apiLockAop.lock(joinPoint) }
            .isInstanceOf(ApiLockAcquisitionException::class.java)
            .hasMessageContaining("LOCK:hello")

        verify(rLock, never()).unlock()
        verify(aopForTransaction, never()).proceed(any())
    }

    @Test
    fun lock_proceedsAndUnlocks_whenAcquired() {
        stubJoinPoint("withString")
        whenever(redissonClient.getLock("LOCK:hello")).thenReturn(rLock)
        whenever(rLock.tryLock(anyLong(), any<TimeUnit>())).thenReturn(true)
        whenever(rLock.isHeldByCurrentThread).thenReturn(true)
        whenever(aopForTransaction.proceed(joinPoint)).thenReturn("ok")

        val result = apiLockAop.lock(joinPoint)

        assertThat(result).isEqualTo("ok")
        verify(aopForTransaction, times(1)).proceed(joinPoint)
        verify(rLock, times(1)).unlock()
    }

    @Test
    fun lock_usesWatchdogTryLock_whenLeaseTimeIsDefault() {
        stubJoinPoint("withString")
        whenever(redissonClient.getLock("LOCK:hello")).thenReturn(rLock)
        whenever(rLock.tryLock(anyLong(), any<TimeUnit>())).thenReturn(true)
        whenever(rLock.isHeldByCurrentThread).thenReturn(true)
        whenever(aopForTransaction.proceed(joinPoint)).thenReturn("ok")

        apiLockAop.lock(joinPoint)

        verify(rLock, times(1)).tryLock(15L, TimeUnit.SECONDS)
        verify(rLock, never()).tryLock(anyLong(), anyLong(), any<TimeUnit>())
    }

    @Test
    fun lock_usesFixedLeaseTryLock_whenLeaseTimeIsPositive() {
        stubJoinPoint("withFixedLeaseTime")
        whenever(redissonClient.getLock("LOCK:hello")).thenReturn(rLock)
        whenever(rLock.tryLock(anyLong(), anyLong(), any<TimeUnit>())).thenReturn(true)
        whenever(rLock.isHeldByCurrentThread).thenReturn(true)
        whenever(aopForTransaction.proceed(joinPoint)).thenReturn("ok")

        val result = apiLockAop.lock(joinPoint)

        assertThat(result).isEqualTo("ok")
        verify(rLock, times(1)).tryLock(15L, 5L, TimeUnit.SECONDS)
        verify(rLock, never()).tryLock(anyLong(), any<TimeUnit>())
        verify(aopForTransaction, times(1)).proceed(joinPoint)
        verify(rLock, times(1)).unlock()
    }

    @Test
    fun lock_throwsApiLockAcquisitionException_whenFixedLeaseTryLockFails() {
        stubJoinPoint("withFixedLeaseTime")
        whenever(redissonClient.getLock("LOCK:hello")).thenReturn(rLock)
        whenever(rLock.tryLock(anyLong(), anyLong(), any<TimeUnit>())).thenReturn(false)

        assertThatThrownBy { apiLockAop.lock(joinPoint) }
            .isInstanceOf(ApiLockAcquisitionException::class.java)
            .hasMessageContaining("LOCK:hello")

        verify(rLock, never()).unlock()
        verify(aopForTransaction, never()).proceed(any())
    }

    @Test
    fun lock_failsOpen_whenGetLockThrowsRedisConnectionException() {
        stubJoinPoint("withString")
        whenever(redissonClient.getLock("LOCK:hello")).thenThrow(RedisConnectionException("connect refused"))
        whenever(aopForTransaction.proceed(joinPoint)).thenReturn("ok")

        val result = apiLockAop.lock(joinPoint)

        assertThat(result).isEqualTo("ok")
        verify(aopForTransaction, times(1)).proceed(joinPoint)
        verify(rLock, never()).tryLock(anyLong(), anyLong(), any<TimeUnit>())
        verify(rLock, never()).unlock()
    }

    @Test
    fun lock_failsOpen_whenTryLockThrowsRedisTimeoutException() {
        stubJoinPoint("withString")
        whenever(redissonClient.getLock("LOCK:hello")).thenReturn(rLock)
        doThrow(RedisTimeoutException("timed out")).whenever(rLock).tryLock(anyLong(), any<TimeUnit>())
        whenever(aopForTransaction.proceed(joinPoint)).thenReturn("ok")

        val result = apiLockAop.lock(joinPoint)

        assertThat(result).isEqualTo("ok")
        verify(aopForTransaction, times(1)).proceed(joinPoint)
        verify(rLock, never()).unlock()
    }

    @Test
    fun lock_rethrows_whenFailOpenIsFalse_andRedisUnavailable() {
        stubJoinPoint("withStringFailClosed")
        whenever(redissonClient.getLock("LOCK:hello")).thenThrow(RedisConnectionException("connect refused"))

        assertThatThrownBy { apiLockAop.lock(joinPoint) }
            .isInstanceOf(RedisConnectionException::class.java)

        verify(aopForTransaction, never()).proceed(any())
    }

    @Test
    fun lock_stillThrowsApiLockAcquisitionException_whenTryLockReturnsFalse_notARedisOutage() {
        stubJoinPoint("withString")
        whenever(redissonClient.getLock("LOCK:hello")).thenReturn(rLock)
        whenever(rLock.tryLock(anyLong(), any<TimeUnit>())).thenReturn(false)

        assertThatThrownBy { apiLockAop.lock(joinPoint) }
            .isInstanceOf(ApiLockAcquisitionException::class.java)

        verify(rLock, never()).unlock()
        verify(aopForTransaction, never()).proceed(any())
    }

    @Test
    fun lock_returnsBusinessResult_whenUnlockThrowsRedisException() {
        stubJoinPoint("withString")
        whenever(redissonClient.getLock("LOCK:hello")).thenReturn(rLock)
        whenever(rLock.tryLock(anyLong(), any<TimeUnit>())).thenReturn(true)
        whenever(rLock.isHeldByCurrentThread).thenReturn(true)
        doThrow(RedisConnectionException("connect refused")).whenever(rLock).unlock()
        whenever(aopForTransaction.proceed(joinPoint)).thenReturn("ok")

        val result = apiLockAop.lock(joinPoint)

        assertThat(result).isEqualTo("ok")
        verify(aopForTransaction, times(1)).proceed(joinPoint)
        verify(rLock, times(1)).unlock()
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    private class Fixture {

        @ApiLock
        fun withLockable(@LockParam value: MyLockable) {
        }

        @ApiLock
        fun withString(@LockParam value: String) {
        }

        @ApiLock(leaseTime = 5)
        fun withFixedLeaseTime(@LockParam value: String) {
        }

        @ApiLock(failOpen = false)
        fun withStringFailClosed(@LockParam value: String) {
        }

        @ApiLock
        fun withTwoParams(@LockParam a: String, @LockParam b: String) {
        }

        class MyLockable(override val key: String) : Lockable
    }
}
