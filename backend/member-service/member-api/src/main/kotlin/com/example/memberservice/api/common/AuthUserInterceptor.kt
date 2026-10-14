package com.example.memberservice.api.common

import com.example.memberservice.application.common.exception.CustomException
import com.example.memberservice.application.common.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping

/**
 * 앱 API(`/api-public/`**) 본인 확인. 게이트웨이가 토큰을 검증하고 subject 를 `X-Auth-User-Id` 로 넣어 준다
 * (클라이언트가 보낸 같은 이름의 헤더는 게이트웨이가 지운다).
 * - 헤더가 없거나 비었으면 403 — 게이트웨이를 거치지 않은 요청(서비스 포트 직접 호출)이다.
 * - 경로에 `{userId}` 가 있으면 헤더와 같아야 한다. 다르면 403.
 *
 * 이 서비스의 앱 API 에서 경로 변수 `{userId}` 는 언제나 "나"다(프로필 수정·탈퇴·친구 목록/추가/이름/즐겨찾기/숨김/차단).
 * 남을 찾는 API 는 다른 변수 이름을 쓴다 — `{email}`(이메일로 회원 조회·친구 찾기), `{id}`(회원 id 로 조회),
 * `{friendMemberId}`(내 친구 한 명) — 그래서 이 검사에 걸리지 않는다. 남의 userId 를 경로로 받는 앱 API 를 새로 만들면
 * 변수 이름을 `userId` 로 짓지 않는다.
 *
 * 컨트롤러는 통과한 헤더 값을 `@RequestHeader(AUTH_USER_ID_HEADER)` 로 받는다.
 * 본문에 userId 를 받는 공개 API 를 만들면 [requireSelf] 로 같은 검사를 한다.
 */
@Component
class AuthUserInterceptor : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val authUserId = request.getHeader(AUTH_USER_ID_HEADER)
        if (authUserId.isNullOrBlank()) throw CustomException(ErrorCode.FORBIDDEN)
        val pathVariables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as? Map<*, *>
        pathVariables?.get(USER_ID_PATH_VARIABLE)?.let { requireSelf(authUserId, it.toString()) }
        return true
    }

    companion object {
        const val AUTH_USER_ID_HEADER = "X-Auth-User-Id"
        const val PUBLIC_PATH_PATTERN = "/api-public/**"
        private const val USER_ID_PATH_VARIABLE = "userId"

        fun requireSelf(authUserId: String, userId: String) {
            if (authUserId != userId) throw CustomException(ErrorCode.FORBIDDEN)
        }
    }
}
