package com.example.memberservice.application.common.lock

import com.example.memberservice.application.config.RwJpaConfig
import org.aspectj.lang.ProceedingJoinPoint
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** @ApiLock 본문을 감싸는 새 트랜잭션. 락 안에서 하는 일은 쓰기이므로 master(rw) 트랜잭션이다. */
@Component
class AopForTransaction {

    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, propagation = Propagation.REQUIRES_NEW)
    @Throws(Throwable::class)
    fun proceed(joinPoint: ProceedingJoinPoint): Any? = joinPoint.proceed()
}
