package com.example.chatservice.application.common.lock

/**
 * 대기 시간 안에 락을 얻지 못했다. 같은 키로 요청이 몰려 처리 중이라는 뜻이다.
 * 전역 예외 처리기(chat-api GlobalExceptionHandler)가 503 "요청이 몰려…" 로 답해 클라이언트가 잠시 뒤 다시 시도하게 한다.
 */
class ApiLockAcquisitionException(key: String) : RuntimeException("락을 얻지 못했습니다: $key")
