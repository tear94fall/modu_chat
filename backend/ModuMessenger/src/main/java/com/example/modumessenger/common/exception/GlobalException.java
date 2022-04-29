package com.example.modumessenger.common.exception;

import lombok.Getter;

@Getter
public class GlobalException extends RuntimeException {

    private final GlobalErrorCode codeAndMessage = GlobalErrorCode
            .findByClass(this.getClass());

    private String code;
    private String message;

    public GlobalException() {
        this.code = codeAndMessage.getCode();
        this.message = codeAndMessage.getMessage();
    }
}
