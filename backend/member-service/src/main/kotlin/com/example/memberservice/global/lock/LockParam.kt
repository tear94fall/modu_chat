package com.example.memberservice.global.lock

/**
 * 이 파라미터의 값이 락 키가 된다. [Lockable] 이면 key 를 쓴다.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class LockParam
