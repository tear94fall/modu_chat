package com.example.memberservice.aop

import com.example.memberservice.global.lock.ApiLock
import com.example.memberservice.global.lock.ApiLockAcquisitionException
import com.example.memberservice.global.lock.LockParam
import com.example.memberservice.global.lock.Lockable
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.UUID
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.redisson.client.RedisException
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Aspect
@Component
// 트랜잭션 어드바이스보다 먼저 실행돼 락이 트랜잭션을 감싸도록
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiLockAop(
    private val redissonClient: RedissonClient,
    private val aopForTransaction: AopForTransaction,
) {

    private val log = LoggerFactory.getLogger(ApiLockAop::class.java)

    @Around("@annotation(com.example.memberservice.global.lock.ApiLock)")
    @Throws(Throwable::class)
    fun lock(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val apiLock = method.getAnnotation(ApiLock::class.java)

        val key = resolveKey(method, joinPoint.args, apiLock.prefix)

        val rLock: RLock = try {
            redissonClient.getLock(key)
        } catch (e: RedisException) {
            // RedissonShutdownException 도 RedisException 의 하위 타입이라 여기서 함께 잡힌다.
            return handleRedisFailure(joinPoint, apiLock, key, e)
        }

        var acquired = false
        try {
            try {
                // leaseTime <= 0 이면 임대 없이 tryLock 해 워치독이 살아있는 동안 자동 연장하게 하고, 그 외엔 지정한 leaseTime 으로 강제 만료시킨다.
                acquired = if (apiLock.leaseTime <= 0) {
                    rLock.tryLock(apiLock.waitTime, apiLock.timeUnit)
                } else {
                    rLock.tryLock(apiLock.waitTime, apiLock.leaseTime, apiLock.timeUnit)
                }
            } catch (e: RedisException) {
                // 락 획득 시도 중 Redis 장애. "락 사용 중"(tryLock == false) 과는 다르게 다룬다.
                return handleRedisFailure(joinPoint, apiLock, key, e)
            }

            if (!acquired) {
                throw ApiLockAcquisitionException(key)
            }

            return aopForTransaction.proceed(joinPoint)
        } finally {
            if (acquired) {
                safeUnlock(rLock, method, key)
            }
        }
    }

    /**
     * Redis 장애(락 획득/조회 단계) 발생 시 fail-open 여부를 결정한다.
     * failOpen() 이 true 면 락 없이 그대로 진행하고, false 면 예외를 그대로 던진다.
     */
    @Throws(Throwable::class)
    private fun handleRedisFailure(joinPoint: ProceedingJoinPoint, apiLock: ApiLock, key: String, e: RedisException): Any? {
        if (!apiLock.failOpen) {
            throw e
        }
        log.warn("Redis 장애로 락 없이 진행합니다 key={} message={}", key, e.message)
        return aopForTransaction.proceed(joinPoint)
    }

    /**
     * unlock() 이 비즈니스 결과를 가리지 않도록, 실패는 로그만 남기고 삼킨다.
     */
    private fun safeUnlock(rLock: RLock, method: Method, key: String) {
        try {
            if (rLock.isHeldByCurrentThread) {
                rLock.unlock()
            }
        } catch (e: IllegalMonitorStateException) {
            log.info("Redisson Lock Already UnLock method={} key={}", method.name, key)
        } catch (e: RedisException) {
            log.warn(
                "Redisson Lock unlock 중 Redis 오류가 발생해 무시합니다 method={} key={} message={}",
                method.name, key, e.message,
            )
        }
    }

    /**
     * [LockParam] 이 붙은 파라미터들로부터 락 키를 만든다.
     * 모듈 안에 열어두어 AOP 프록시 없이 단위 테스트할 수 있게 한다.
     */
    internal fun resolveKey(method: Method, args: Array<Any?>, prefix: String?): String {
        val parameters = method.parameters
        val joined = StringBuilder()

        for (i in parameters.indices) {
            if (!hasLockParam(parameters[i])) {
                continue
            }

            val part = toKeyPart(parameters[i], args[i])
            if (joined.isNotEmpty()) {
                joined.append(KEY_DELIMITER)
            }
            joined.append(part)
        }

        val namespace = if (prefix.isNullOrBlank()) "" else prefix + KEY_DELIMITER
        return REDISSON_LOCK_PREFIX + namespace + joined
    }

    private fun hasLockParam(parameter: Parameter): Boolean = parameter.getAnnotation(LockParam::class.java) != null

    private fun toKeyPart(parameter: Parameter, arg: Any?): String {
        val paramName = parameter.name

        if (arg == null) {
            throw IllegalArgumentException("@LockParam 값이 null 입니다: $paramName")
        }

        if (arg is Lockable) {
            val key = arg.key
            if (key.isBlank()) {
                throw IllegalArgumentException("Lockable.getKey() 가 비어 있습니다: ${arg.javaClass.name}")
            }
            return key
        }

        if (arg is CharSequence || arg is Number || arg is Boolean || arg is Char || arg is Enum<*> || arg is UUID) {
            val value = arg.toString()
            if (value.isBlank()) {
                throw IllegalArgumentException("@LockParam 값이 비어 있습니다: $paramName")
            }
            return value
        }

        throw IllegalArgumentException(
            "@LockParam 은 Lockable 을 구현하거나 String·기본형·enum·UUID 여야 키를 만들 수 있습니다: " + arg.javaClass.name,
        )
    }

    companion object {
        private const val REDISSON_LOCK_PREFIX = "LOCK:"
        private const val KEY_DELIMITER = ":"
    }
}
