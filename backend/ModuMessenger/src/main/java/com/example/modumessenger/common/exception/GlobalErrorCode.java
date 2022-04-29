package com.example.modumessenger.common.exception;

import com.example.modumessenger.auth.exception.TokenExpiredException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum GlobalErrorCode {
    ERROR_NOT_FOUND_ERROR_CODE("0001", "발생한 에러의 에러코드를 찾을 수 없습니다.", NotFoundErrorCodeException.class),
    ERROR_NOT_FOUND_API("0002", "해당 경로에 대한 응답 API를 찾을 수 없습니다.", NoHandlerFoundException.class),
    /**
     *
     */

    ERROR_EXPIRED_TOKEN("1024", "토큰 유효기간이 만료되었습니다.", TokenExpiredException .class)
    ;

    private final String code;
    private final String message;
    private final Class<? extends Exception> type;

    public static GlobalErrorCode findByClass(Class<? extends Exception> type) {
        return Arrays.stream(GlobalErrorCode.values())
                .filter(code -> code.type.equals(type))
                .findAny()
                .orElseThrow(NotFoundErrorCodeException::new);
    }

}