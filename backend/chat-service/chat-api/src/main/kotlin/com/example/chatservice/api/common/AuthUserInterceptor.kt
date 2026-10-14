package com.example.chatservice.api.common

import com.example.chatservice.application.common.exception.CustomException
import com.example.chatservice.application.common.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * 앱 API(`/api-public/`**) 본인 확인의 첫 단계. 게이트웨이가 토큰을 검증하고 subject 를 `X-Auth-User-Id` 로 넣어 준다
 * (클라이언트가 보낸 같은 이름의 헤더는 게이트웨이가 지운다).
 * 헤더가 없거나 비었으면 403 — 게이트웨이를 거치지 않은 요청(서비스 포트 직접 호출)이다.
 *
 * 경로의 `{userId}` 를 헤더와 글자 그대로 비교하는 일은 여기서 하지 않는다. 채팅 API 는 같은 자리에
 * 안드로이드가 회원 id(숫자 PK)를, iOS 가 userId 를 넣기 때문이다(안 읽은 개수, 읽음 처리).
 * 그 비교와 방 멤버 확인은 회원 id 를 알아야 하므로 유스케이스 앞의 ChatAccessGuard 가 한 곳에서 한다.
 *
 * 컨트롤러는 통과한 헤더 값을 `@RequestHeader(AUTH_USER_ID_HEADER)` 로 받는다.
 */
@Component
class AuthUserInterceptor : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val authUserId = request.getHeader(AUTH_USER_ID_HEADER)
        if (authUserId.isNullOrBlank()) throw CustomException(ErrorCode.FORBIDDEN)
        return true
    }

    companion object {
        const val AUTH_USER_ID_HEADER = "X-Auth-User-Id"
        const val PUBLIC_PATH_PATTERN = "/api-public/**"
    }
}
