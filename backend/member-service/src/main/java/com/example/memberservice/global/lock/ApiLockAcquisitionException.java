package com.example.memberservice.global.lock;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 대기 시간 안에 락을 얻지 못했다. 다른 요청이 같은 키로 처리 중이라는 뜻이므로
 * 서버 오류(500)가 아니라 409 로 내려 클라이언트가 재시도를 판단하게 한다.
 *
 * <p>이 서비스에는 전역 예외 처리기가 없어 @ResponseStatus 로 직접 지정한다.</p>
 */
@ResponseStatus(value = HttpStatus.CONFLICT, reason = "같은 요청이 처리 중입니다. 잠시 후 다시 시도해 주세요.")
public class ApiLockAcquisitionException extends RuntimeException {

    public ApiLockAcquisitionException(String key) {
        super("락을 얻지 못했습니다: " + key);
    }
}
