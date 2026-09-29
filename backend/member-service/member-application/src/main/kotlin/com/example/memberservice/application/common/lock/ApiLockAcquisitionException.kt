package com.example.memberservice.application.common.lock

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * 대기 시간 안에 락을 얻지 못했다. 다른 요청이 같은 키로 처리 중이라는 뜻이므로
 * 서버 오류(500)가 아니라 409 로 내려 클라이언트가 재시도를 판단하게 한다.
 *
 * 상태는 @ResponseStatus 로 지정한다. 전역 예외 처리기(GlobalExceptionHandler)가 이 애노테이션을 읽어 409 로 답한다.
 */
@ResponseStatus(value = HttpStatus.CONFLICT, reason = "같은 요청이 처리 중입니다. 잠시 후 다시 시도해 주세요.")
class ApiLockAcquisitionException(key: String) : RuntimeException("락을 얻지 못했습니다: $key")
