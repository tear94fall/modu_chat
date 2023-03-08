package com.example.authservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 200
    SUCCESS(OK, "OK"),

    // 400
    NOT_SUPPORTED_HTTP_METHOD(BAD_REQUEST,"지원하지 않는 Http Method 방식입니다."),
    NOT_VALID_METHOD_ARGUMENT(BAD_REQUEST,"유효하지 않은 Request Body 혹은 Argument 입니다."),
    EMAIL_NOT_FOUND(BAD_REQUEST, "email을 찾을 수 없습니다."),
    USERID_NOT_FOUND(BAD_REQUEST, "해당 사용자를 찾을 수 없습니다."),
    INVALID_CHAT_ROOM_MEMBER(BAD_REQUEST, "채팅방 멤버가 아닙니다."),

    // 401
    UNAUTHORIZED_TOKEN_ERROR(UNAUTHORIZED, "인증되지 않은 토큰 입니다."),

    // 404
    USERID_NOT_FOUND_ERROR(NOT_FOUND, "해당 사용자 아이디를 찾을수 없습니다."),
    CHAT_NOT_FOUND_ERROR(NOT_FOUND, "채팅을 찾을수 없습니다."),
    CHATROOM_NOT_FOUND_ERROR(NOT_FOUND, "채팅방을 찾을수 없습니다."),
    USERID_FRIENDS_NOT_FOUND_ERROR(NOT_FOUND, "회원님의 아이디로 등록된 친구를 찾을 수 없습니다."),
    USEREMAIL_FRIENDS_NOT_FOUND_ERROR(NOT_FOUND, "회원님의 메일로 등록된 친구를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
