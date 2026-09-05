package com.example.memberservice.global.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 분산락을 거는 메서드. 키는 {@link LockParam} 을 붙인 파라미터에서 만든다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiLock {

    /**
     * 키 앞에 붙일 이름공간. 비우면 붙이지 않는다.
     */
    String prefix() default "";

    /**
     * 락의 시간 단위
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 락을 기다리는 시간 (default - 15s)
     * 락 획득을 위해 waitTime 만큼 대기한다
     */
    long waitTime() default 15L;

    /**
     * 락 임대 시간 (default - -1)
     * -1(기본) 이면 Redisson 워치독이 락을 잡고 있는 동안 임대를 자동 연장하고,
     * 프로세스가 죽으면 자동으로 풀린다. 작업 시간을 예측할 수 없을 때 쓴다.
     * 0 보다 크면 그 시간이 지나면 강제로 풀린다. 작업 시간이 확실히 짧을 때만 쓴다.
     */
    long leaseTime() default -1L;

    /**
     * Redis 에 문제가 생기면 락 없이 진행할지. 기본은 진행한다.
     */
    boolean failOpen() default true;
}
