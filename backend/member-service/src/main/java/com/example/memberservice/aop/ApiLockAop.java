package com.example.memberservice.aop;

import com.example.memberservice.global.lock.ApiLock;
import com.example.memberservice.global.lock.ApiLockAcquisitionException;
import com.example.memberservice.global.lock.LockParam;
import com.example.memberservice.global.lock.Lockable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
// 트랜잭션 어드바이스보다 먼저 실행돼 락이 트랜잭션을 감싸도록
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiLockAop {

    private static final String REDISSON_LOCK_PREFIX = "LOCK:";
    private static final String KEY_DELIMITER = ":";

    private final RedissonClient redissonClient;
    private final AopForTransaction aopForTransaction;

    @Around("@annotation(com.example.memberservice.global.lock.ApiLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        ApiLock apiLock = method.getAnnotation(ApiLock.class);

        String key = resolveKey(method, joinPoint.getArgs(), apiLock.prefix());

        RLock rLock;
        try {
            rLock = redissonClient.getLock(key);
        } catch (RedisException e) {
            // RedissonShutdownException 도 RedisException 의 하위 타입이라 여기서 함께 잡힌다.
            return handleRedisFailure(joinPoint, apiLock, key, e);
        }

        boolean acquired = false;
        try {
            try {
                // leaseTime <= 0 이면 임대 없이 tryLock 해 워치독이 살아있는 동안 자동 연장하게 하고, 그 외엔 지정한 leaseTime 으로 강제 만료시킨다.
                acquired = apiLock.leaseTime() <= 0
                        ? rLock.tryLock(apiLock.waitTime(), apiLock.timeUnit())
                        : rLock.tryLock(apiLock.waitTime(), apiLock.leaseTime(), apiLock.timeUnit());
            } catch (RedisException e) {
                // 락 획득 시도 중 Redis 장애. "락 사용 중"(tryLock == false) 과는 다르게 다룬다.
                return handleRedisFailure(joinPoint, apiLock, key, e);
            }

            if (!acquired) {
                throw new ApiLockAcquisitionException(key);
            }

            return aopForTransaction.proceed(joinPoint);
        } finally {
            if (acquired) {
                safeUnlock(rLock, method, key);
            }
        }
    }

    /**
     * Redis 장애(락 획득/조회 단계) 발생 시 fail-open 여부를 결정한다.
     * failOpen() 이 true 면 락 없이 그대로 진행하고, false 면 예외를 그대로 던진다.
     */
    private Object handleRedisFailure(ProceedingJoinPoint joinPoint, ApiLock apiLock, String key, RedisException e) throws Throwable {
        if (!apiLock.failOpen()) {
            throw e;
        }
        log.warn("Redis 장애로 락 없이 진행합니다 key={} message={}", key, e.getMessage());
        return aopForTransaction.proceed(joinPoint);
    }

    /**
     * unlock() 이 비즈니스 결과를 가리지 않도록, 실패는 로그만 남기고 삼킨다.
     */
    private void safeUnlock(RLock rLock, Method method, String key) {
        try {
            if (rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        } catch (IllegalMonitorStateException e) {
            log.info("Redisson Lock Already UnLock method={} key={}", method.getName(), key);
        } catch (RedisException e) {
            log.warn("Redisson Lock unlock 중 Redis 오류가 발생해 무시합니다 method={} key={} message={}",
                    method.getName(), key, e.getMessage());
        }
    }

    /**
     * {@link LockParam} 이 붙은 파라미터들로부터 락 키를 만든다.
     * 패키지 프라이빗으로 열어두어 AOP 프록시 없이 단위 테스트할 수 있게 한다.
     */
    String resolveKey(Method method, Object[] args, String prefix) {
        Parameter[] parameters = method.getParameters();
        StringBuilder joined = new StringBuilder();

        for (int i = 0; i < parameters.length; i++) {
            if (!hasLockParam(parameters[i])) {
                continue;
            }

            String part = toKeyPart(parameters[i], args[i]);
            if (joined.length() > 0) {
                joined.append(KEY_DELIMITER);
            }
            joined.append(part);
        }

        String namespace = (prefix == null || prefix.isBlank()) ? "" : prefix + KEY_DELIMITER;
        return REDISSON_LOCK_PREFIX + namespace + joined;
    }

    private boolean hasLockParam(Parameter parameter) {
        return parameter.getAnnotation(LockParam.class) != null;
    }

    private String toKeyPart(Parameter parameter, Object arg) {
        String paramName = parameter.getName();

        if (arg == null) {
            throw new IllegalArgumentException("@LockParam 값이 null 입니다: " + paramName);
        }

        if (arg instanceof Lockable lockable) {
            String key = lockable.getKey();
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Lockable.getKey() 가 비어 있습니다: " + arg.getClass().getName());
            }
            return key;
        }

        if (arg instanceof CharSequence
                || arg instanceof Number
                || arg instanceof Boolean
                || arg instanceof Character
                || arg instanceof Enum
                || arg instanceof UUID) {
            String value = String.valueOf(arg);
            if (value.isBlank()) {
                throw new IllegalArgumentException("@LockParam 값이 비어 있습니다: " + paramName);
            }
            return value;
        }

        throw new IllegalArgumentException(
                "@LockParam 은 Lockable 을 구현하거나 String·기본형·enum·UUID 여야 키를 만들 수 있습니다: "
                        + arg.getClass().getName());
    }
}
