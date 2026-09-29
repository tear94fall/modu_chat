package com.example.profileservice.api.internal

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.UrlPathHelper
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * `/api-internal`, `/api-debug`, `/api-admin` 아래 경로는 앱이 직접 부르지 않는다. 게이트웨이에 라우트가 없어
 * 외부에서는 못 오지만, 서비스 포트로 직접 오는 요청은 막을 수 없으므로 여기서
 * X-Internal-Token 을 검사한다. 토큰은 modu.internal-api.token 이다.
 *
 * 경로 비교는 원시 URI 가 아니라 Spring 이 핸들러 매핑에 쓰는 것과 같은
 * 디코딩·정규화된 경로로 한다. 원시 URI 로 비교하면 //api-internal, /%61pi-internal,
 * /api-internal;x=1, /api-public/../api-internal 이 필터를 건너뛴다.
 */
@Component
class InternalApiFilter(@Value("\${modu.internal-api.token}") expectedToken: String) : OncePerRequestFilter() {

    private val expectedToken: ByteArray

    init {
        check(StringUtils.hasText(expectedToken)) { "modu.internal-api.token 이 비어 있다. 빈 토큰은 누구나 통과시키므로 기동을 거부한다." }
        this.expectedToken = expectedToken.toByteArray(StandardCharsets.UTF_8)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean = !isGuarded(normalizedPath(request))

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val token = request.getHeader(HEADER)
        if (token == null || !MessageDigest.isEqual(token.toByteArray(StandardCharsets.UTF_8), expectedToken)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "internal api token required")
            return
        }
        chain.doFilter(request, response)
    }

    companion object {
        const val HEADER = "X-Internal-Token"
        const val INTERNAL_PREFIX = "/api-internal/"
        const val DEBUG_PREFIX = "/api-debug/"
        const val ADMIN_PREFIX = "/api-admin/"

        /** 하위 호환: 기존 테스트가 참조한다. */
        const val PREFIX = INTERNAL_PREFIX

        private val PATH_HELPER = UrlPathHelper()

        /** 디코딩하고 ;파라미터를 떼고 // 와 .. 을 정리한 경로. */
        @JvmStatic
        fun normalizedPath(request: HttpServletRequest): String {
            val path = PATH_HELPER.getPathWithinApplication(request)
            return StringUtils.cleanPath(path.replace(Regex("/{2,}"), "/"))
        }

        @JvmStatic
        fun isGuarded(normalizedPath: String): Boolean =
            normalizedPath.startsWith(INTERNAL_PREFIX) || normalizedPath.startsWith(DEBUG_PREFIX) ||
                normalizedPath.startsWith(ADMIN_PREFIX) ||
                normalizedPath == "/api-internal" || normalizedPath == "/api-debug" || normalizedPath == "/api-admin"
    }
}
