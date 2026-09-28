package com.example.authservice.oauth.usage

import com.nimbusds.jwt.SignedJWT
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

/**
 * 토큰 엔드포인트의 성공 처리기. 원래 처리기([delegate])로 토큰 응답을 먼저 쓴 뒤, [grants] 로 발급한 경우
 * 토큰 subject(회원 userId)와 등록 클라이언트 ID 를 [notifier] 로 넘긴다. 여기서 무슨 일이 나도 토큰 응답은 그대로다.
 *
 * subject 는 발급한 액세스 토큰(JWT)에서 읽는다. refresh_token grant 도 원래 인가의 principal 로 토큰을 만들므로 같은 값이다.
 */
class UsageRecordingTokenResponseHandler(
    private val delegate: AuthenticationSuccessHandler,
    private val notifier: MemberUsageNotifier,
    private val grants: Set<String>,
) : AuthenticationSuccessHandler {

    private val log = LoggerFactory.getLogger(UsageRecordingTokenResponseHandler::class.java)

    override fun onAuthenticationSuccess(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication) {
        delegate.onAuthenticationSuccess(request, response, authentication)
        try {
            val grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE) ?: return
            if (grantType !in grants) return
            val token = authentication as? OAuth2AccessTokenAuthenticationToken ?: return
            val userId = SignedJWT.parse(token.accessToken.tokenValue).jwtClaimsSet.subject ?: return
            notifier.notify(userId, token.registeredClient.clientId)
        } catch (e: Exception) {
            log.warn("서비스 이용 기록을 넘기지 못함 message={}", e.message)
        }
    }

    companion object {
        /** 이용 기록을 남기는 grant: 구글 ID 토큰, 앱 간 SSO 코드, 리프레시 토큰. */
        @JvmStatic
        fun defaultGrants(vararg custom: AuthorizationGrantType): Set<String> =
            (custom.map { it.value } + AuthorizationGrantType.REFRESH_TOKEN.value).toSet()
    }
}
