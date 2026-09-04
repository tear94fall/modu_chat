package com.example.memberservice.aop;

import com.example.memberservice.global.lock.ApiLock;
import com.example.memberservice.global.lock.ApiLockAcquisitionException;
import com.example.memberservice.global.lock.LockParam;
import com.example.memberservice.global.lock.Lockable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisConnectionException;
import org.redisson.client.RedisTimeoutException;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.willThrow;

/**
 * ApiLockAop 의 키 조합 로직과 락 획득 성공/실패 흐름을 검증한다.
 * MethodSignature 를 완전히 흉내내기 까다로운 부분(파라미터 애노테이션 조회)이 있어,
 * 키 조합은 패키지 프라이빗 {@code resolveKey} 를 직접 호출해 검증하고,
 * tryLock 성공/실패에 따른 흐름은 joinPoint/signature 를 모킹한 종단 테스트로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ApiLockAopTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private AopForTransaction aopForTransaction;
    @Mock
    private RLock rLock;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private MethodSignature methodSignature;

    private ApiLockAop apiLockAop;

    @BeforeEach
    void setUp() {
        apiLockAop = new ApiLockAop(redissonClient, aopForTransaction);
    }

    @Test
    void resolveKey_fromLockableArgument() throws NoSuchMethodException {
        Method method = Fixture.class.getDeclaredMethod("withLockable", Fixture.MyLockable.class);

        String key = apiLockAop.resolveKey(method, new Object[]{new Fixture.MyLockable("abc")}, "");

        assertThat(key).isEqualTo("LOCK:abc");
    }

    @Test
    void resolveKey_fromStringArgument() throws NoSuchMethodException {
        Method method = Fixture.class.getDeclaredMethod("withString", String.class);

        String key = apiLockAop.resolveKey(method, new Object[]{"hello"}, "");

        assertThat(key).isEqualTo("LOCK:hello");
    }

    @Test
    void resolveKey_withPrefix() throws NoSuchMethodException {
        Method method = Fixture.class.getDeclaredMethod("withString", String.class);

        String key = apiLockAop.resolveKey(method, new Object[]{"hello"}, "x");

        assertThat(key).isEqualTo("LOCK:x:hello");
    }

    @Test
    void resolveKey_joinsMultipleLockParamsWithColon() throws NoSuchMethodException {
        Method method = Fixture.class.getDeclaredMethod("withTwoParams", String.class, String.class);

        String key = apiLockAop.resolveKey(method, new Object[]{"a", "b"}, "");

        assertThat(key).isEqualTo("LOCK:a:b");
    }

    @Test
    void lock_throwsApiLockAcquisitionException_whenTryLockFails_andNeverUnlocks() throws Throwable {
        Method method = Fixture.class.getDeclaredMethod("withString", String.class);
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getMethod()).willReturn(method);
        given(joinPoint.getArgs()).willReturn(new Object[]{"hello"});
        given(redissonClient.getLock("LOCK:hello")).willReturn(rLock);
        given(rLock.tryLock(anyLong(), any(TimeUnit.class))).willReturn(false);

        assertThatThrownBy(() -> apiLockAop.lock(joinPoint))
                .isInstanceOf(ApiLockAcquisitionException.class)
                .hasMessageContaining("LOCK:hello");

        verify(rLock, never()).unlock();
        verify(aopForTransaction, never()).proceed(any());
    }

    @Test
    void lock_proceedsAndUnlocks_whenAcquired() throws Throwable {
        Method method = Fixture.class.getDeclaredMethod("withString", String.class);
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getMethod()).willReturn(method);
        given(joinPoint.getArgs()).willReturn(new Object[]{"hello"});
        given(redissonClient.getLock("LOCK:hello")).willReturn(rLock);
        given(rLock.tryLock(anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        given(aopForTransaction.proceed(joinPoint)).willReturn("ok");

        Object result = apiLockAop.lock(joinPoint);

        assertThat(result).isEqualTo("ok");
        verify(aopForTransaction, times(1)).proceed(joinPoint);
        verify(rLock, times(1)).unlock();
    }

    @Test
    void lock_usesWatchdogTryLock_whenLeaseTimeIsDefault() throws Throwable {
        Method method = Fixture.class.getDeclaredMethod("withString", String.class);
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getMethod()).willReturn(method);
        given(joinPoint.getArgs()).willReturn(new Object[]{"hello"});
        given(redissonClient.getLock("LOCK:hello")).willReturn(rLock);
        given(rLock.tryLock(anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        given(aopForTransaction.proceed(joinPoint)).willReturn("ok");

        apiLockAop.lock(joinPoint);

        verify(rLock, times(1)).tryLock(15L, TimeUnit.SECONDS);
        verify(rLock, never()).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void lock_usesFixedLeaseTryLock_whenLeaseTimeIsPositive() throws Throwable {
        Method method = Fixture.class.getDeclaredMethod("withFixedLeaseTime", String.class);
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getMethod()).willReturn(method);
        given(joinPoint.getArgs()).willReturn(new Object[]{"hello"});
        given(redissonClient.getLock("LOCK:hello")).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        given(aopForTransaction.proceed(joinPoint)).willReturn("ok");

        Object result = apiLockAop.lock(joinPoint);

        assertThat(result).isEqualTo("ok");
        verify(rLock, times(1)).tryLock(15L, 5L, TimeUnit.SECONDS);
        verify(rLock, never()).tryLock(anyLong(), any(TimeUnit.class));
        verify(aopForTransaction, times(1)).proceed(joinPoint);
        verify(rLock, times(1)).unlock();
    }

    @Test
    void lock_throwsApiLockAcquisitionException_whenFixedLeaseTryLockFails() throws Throwable {
        Method method = Fixture.class.getDeclaredMethod("withFixedLeaseTime", String.class);
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getMethod()).willReturn(method);
        given(joinPoint.getArgs()).willReturn(new Object[]{"hello"});
        given(redissonClient.getLock("LOCK:hello")).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(false);

        assertThatThrownBy(() -> apiLockAop.lock(joinPoint))
                .isInstanceOf(ApiLockAcquisitionException.class)
                .hasMessageContaining("LOCK:hello");

        verify(rLock, never()).unlock();
        verify(aopForTransaction, never()).proceed(any());
    }

    @Test
    void lock_failsOpen_whenGetLockThrowsRedisConnectionException() throws Throwable {
        Method method = Fixture.class.getDeclaredMethod("withString", String.class);
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getMethod()).willReturn(method);
        given(joinPoint.getArgs()).willReturn(new Object[]{"hello"});
        given(redissonClient.getLock("LOCK:hello")).willThrow(new RedisConnectionException("connect refused"));
        given(aopForTransaction.proceed(joinPoint)).willReturn("ok");

        Object result = apiLockAop.lock(joinPoint);

        assertThat(result).isEqualTo("ok");
        verify(aopForTransaction, times(1)).proceed(joinPoint);
        verify(rLock, never()).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
        verify(rLock, never()).unlock();
    }

    @Test
    void lock_failsOpen_whenTryLockThrowsRedisTimeoutException() throws Throwable {
        Method method = Fixture.class.getDeclaredMethod("withString", String.class);
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getMethod()).willReturn(method);
        given(joinPoint.getArgs()).willReturn(new Object[]{"hello"});
        given(redissonClient.getLock("LOCK:hello")).willReturn(rLock);
        willThrow(new RedisTimeoutException("timed out"))
                .given(rLock).tryLock(anyLong(), any(TimeUnit.class));
        given(aopForTransaction.proceed(joinPoint)).willReturn("ok");

        Object result = apiLockAop.lock(joinPoint);

        assertThat(result).isEqualTo("ok");
        verify(aopForTransaction, times(1)).proceed(joinPoint);
        verify(rLock, never()).unlock();
    }

    @Test
    void lock_rethrows_whenFailOpenIsFalse_andRedisUnavailable() throws Throwable {
        Method method = Fixture.class.getDeclaredMethod("withStringFailClosed", String.class);
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getMethod()).willReturn(method);
        given(joinPoint.getArgs()).willReturn(new Object[]{"hello"});
        given(redissonClient.getLock("LOCK:hello")).willThrow(new RedisConnectionException("connect refused"));

        assertThatThrownBy(() -> apiLockAop.lock(joinPoint))
                .isInstanceOf(RedisConnectionException.class);

        verify(aopForTransaction, never()).proceed(any());
    }

    @Test
    void lock_stillThrowsApiLockAcquisitionException_whenTryLockReturnsFalse_notARedisOutage() throws Throwable {
        Method method = Fixture.class.getDeclaredMethod("withString", String.class);
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getMethod()).willReturn(method);
        given(joinPoint.getArgs()).willReturn(new Object[]{"hello"});
        given(redissonClient.getLock("LOCK:hello")).willReturn(rLock);
        given(rLock.tryLock(anyLong(), any(TimeUnit.class))).willReturn(false);

        assertThatThrownBy(() -> apiLockAop.lock(joinPoint))
                .isInstanceOf(ApiLockAcquisitionException.class);

        verify(rLock, never()).unlock();
        verify(aopForTransaction, never()).proceed(any());
    }

    @Test
    void lock_returnsBusinessResult_whenUnlockThrowsRedisException() throws Throwable {
        Method method = Fixture.class.getDeclaredMethod("withString", String.class);
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getMethod()).willReturn(method);
        given(joinPoint.getArgs()).willReturn(new Object[]{"hello"});
        given(redissonClient.getLock("LOCK:hello")).willReturn(rLock);
        given(rLock.tryLock(anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        willThrow(new RedisConnectionException("connect refused")).given(rLock).unlock();
        given(aopForTransaction.proceed(joinPoint)).willReturn("ok");

        Object result = apiLockAop.lock(joinPoint);

        assertThat(result).isEqualTo("ok");
        verify(aopForTransaction, times(1)).proceed(joinPoint);
        verify(rLock, times(1)).unlock();
    }

    private static class Fixture {

        @ApiLock
        void withLockable(@LockParam MyLockable value) {
        }

        @ApiLock
        void withString(@LockParam String value) {
        }

        @ApiLock(leaseTime = 5)
        void withFixedLeaseTime(@LockParam String value) {
        }

        @ApiLock(failOpen = false)
        void withStringFailClosed(@LockParam String value) {
        }

        @ApiLock
        void withTwoParams(@LockParam String a, @LockParam String b) {
        }

        private record MyLockable(String key) implements Lockable {
            @Override
            public String getKey() {
                return key;
            }
        }
    }
}
