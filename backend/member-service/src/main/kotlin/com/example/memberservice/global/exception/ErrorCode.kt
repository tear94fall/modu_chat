package com.example.memberservice.global.exception

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED

enum class ErrorCode(val status: HttpStatus, val message: String) {

    // 200
    SUCCESS(OK, "OK"),

    // 400
    NOT_SUPPORTED_HTTP_METHOD(BAD_REQUEST, "지원하지 않는 Http Method 방식입니다."),
    NOT_VALID_METHOD_ARGUMENT(BAD_REQUEST, "유효하지 않은 Request Body 혹은 Argument 입니다."),
    EMAIL_NOT_FOUND(BAD_REQUEST, "email을 찾을 수 없습니다."),
    USERID_NOT_FOUND(BAD_REQUEST, "해당 사용자를 찾을 수 없습니다."),
    INVALID_CHAT_ROOM_MEMBER(BAD_REQUEST, "채팅방 멤버가 아닙니다."),
    ALREADY_EXIST_USERID(BAD_REQUEST, "이미 존재하는 유저의 email 입니다."),
    CREATE_NEW_USER_FAIL(BAD_REQUEST, "계정 생성에 실패하였습니다."),

    // 401
    UNAUTHORIZED_TOKEN_ERROR(UNAUTHORIZED, "인증되지 않은 토큰 입니다."),
    UNAUTHORIZED_GOOGLE_ID_TOKEN_ERROR(UNAUTHORIZED, "인증되지 않은 구글 id 토큰 입니다."),

    // 404
    USERID_NOT_FOUND_ERROR(NOT_FOUND, "해당 사용자 아이디를 찾을수 없습니다."),
    MEMBER_ID_NOT_FOUND_ERROR(NOT_FOUND, "회원을 찾을수 없습니다."),
    CHAT_NOT_FOUND_ERROR(NOT_FOUND, "채팅을 찾을수 없습니다."),
    CHATROOM_NOT_FOUND_ERROR(NOT_FOUND, "채팅방을 찾을수 없습니다."),
    USERID_FRIENDS_NOT_FOUND_ERROR(NOT_FOUND, "회원님의 아이디로 등록된 친구를 찾을 수 없습니다."),
    USER_EMAIL_FRIENDS_NOT_FOUND_ERROR(NOT_FOUND, "회원님의 메일로 등록된 친구를 찾을 수 없습니다."),

    // 500
    USER_INTERNAL_SERVER_ERROR(INTERNAL_SERVER_ERROR, "사용자 계정 생성에 실패하였습니다."),
    USER_PROFILE_IMAGE_UPLOAD_ERROR(INTERNAL_SERVER_ERROR, "사용자 프로필 이미지 업로드에 실패했습니다."),
    USER_WALLPAPER_IMAGE_UPLOAD_ERROR(INTERNAL_SERVER_ERROR, "사용자 배경 이미지 업로드에 실패했습니다."),
}
