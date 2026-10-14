package com.example.pointservice.api.internal

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.UrlPathHelper

/**
 * `/api-internal`, `/api-debug`, `/api-admin` 아래 경로는 앱이 직접 부르지 않는다. 게이트웨이에 라우트가 없어
 * 외부에서는 못 오지만, 서비스 포트로 직접 오는 요청은 막을 수 없으므로 여기서 X-Internal-Token 을 검사한다.
 * 경로 비교는 디코딩·정규화된 경로로 한다(다른 메신저 서비스와 같은 필터).
 */
@Component
class InternalApiFilter(@Value("\${modu.internal-api.token}") expectedToken: String) : OncePerRequestFilter() {

    private val expectedToken: ByteArray

    init {
        if (!StringUtils.hasText(expectedToken)) {
            throw IllegalStateException("modu.internal-api.token 이 비어 있다. 빈 토큰은 누구나 통과시키므로 기동을 거부한다.")
        }
        this.expectedToken = expectedToken.toByteArray(StandardCharsets.UTF_8)
    }

    companion object {
        const val HEADER = "X-Internal-Token"
        private val GUARDED_PREFIXES = listOf("/api-internal", "/api-debug", "/api-admin")
        private val PATH_HELPER = UrlPathHelper()

        internal fun normalizedPath(request: HttpServletRequest): String {
            val path = PATH_HELPER.getPathWithinApplication(request)
            return StringUtils.cleanPath(path.replace(Regex("/{2,}"), "/"))
        }

        internal fun isGuarded(normalizedPath: String): Boolean =
            GUARDED_PREFIXES.any { normalizedPath == it || normalizedPath.startsWith("$it/") }
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
}
