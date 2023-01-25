package com.example.modumessenger.common.exception;

import lombok.Getter;

import java.util.Set;

@Getter
public class CustomException extends RuntimeException {

    private ErrorCode errorCode;
    private String errorTarget;

    public CustomException(ErrorCode errorCode) {
        this.errorCode = errorCode;
        this.errorTarget = "";
    }

    public CustomException(ErrorCode errorCode, String errorTarget) {
        this.errorCode = errorCode;
        this.errorTarget = errorTarget;
    }

    public CustomException(ErrorCode errorCode, Long id) {
        this.errorCode = errorCode;
        this.errorTarget = String.valueOf(id);
    }

    public CustomException(ErrorCode errorCode, Set<Long> ids) {
        this.errorCode = errorCode;
        this.errorTarget = setToString(ids);
    }

    private String setToString(Set<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for(Long id : ids) {
            sb.append(id).append(",");
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
}