package com.example.authservice.oauth.grant

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.util.StringUtils

/** 커스텀 grant 변환기들의 공통 파싱. */
object GrantRequestParser {

    fun isGrant(request: HttpServletRequest, grantType: String): Boolean =
        grantType == request.getParameter(OAuth2ParameterNames.GRANT_TYPE)

    /** 앞 단계(클라이언트 인증)가 넣어 둔 클라이언트 principal. */
    fun clientPrincipal(): Authentication {
        val a = SecurityContextHolder.getContext().authentication
        if (a is OAuth2ClientAuthenticationToken && a.isAuthenticated) {
            return a
        }
        throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT)
    }

    fun required(request: HttpServletRequest, name: String): String {
        val v = request.getParameter(name)
        if (!StringUtils.hasText(v) || request.getParameterValues(name).size != 1) {
            throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "파라미터가 없거나 중복입니다: $name", null))
        }
        return v
    }

    fun scopes(request: HttpServletRequest): MutableSet<String> {
        val scope = request.getParameter(OAuth2ParameterNames.SCOPE)
        if (!StringUtils.hasText(scope)) {
            return HashSet()
        }
        return scope.trim().split(Regex("\\s+")).toHashSet()
    }
}
