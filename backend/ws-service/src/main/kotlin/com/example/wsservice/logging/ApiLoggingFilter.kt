package com.example.wsservice.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import net.logstash.logback.argument.StructuredArguments.kv
import net.logstash.logback.argument.StructuredArguments.v
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.TreeMap

/**
 * API 요청·응답 전체 로그 설정(`modu.logging.api`).
 * [enabled] 끄면 필터 자체를 등록하지 않는다. [maxBodyBytes] 를 넘는 본문은 잘라서 남긴다(기본 4096, application.yml 이 올린다).
 * [excludePaths] 로 시작하는 경로는 남기지 않는다 — 파일 본문을 흘려보내는 끝점처럼 응답을 메모리에 담으면 안 되는 곳(http.access 는 남는다).
 */
@ConfigurationProperties("modu.logging.api")
data class ApiLoggingProperties(
    val enabled: Boolean = true,
    val maxBodyBytes: Int = DEFAULT_MAX_BODY_BYTES,
    val excludePaths: List<String> = emptyList(),
) {
    companion object {
        const val DEFAULT_MAX_BODY_BYTES = 4096
    }
}

/** 이 서비스는 @ConfigurationPropertiesScan 을 쓰지 않아 여기서 등록한다. */
@Configuration
@EnableConfigurationProperties(ApiLoggingProperties::class)
class ApiLoggingConfig

/**
 * `/api-` 로 시작하는 경로(api-public·api-admin·api-internal·api-debug …) 요청의 입력·출력·헤더 전부를 `api.access` 로거에
 * 한 줄(INFO)로 남긴다. 커머스(commerce-service ApiLoggingFilter)와 같은 모양이라 OpenSearch 에서 같은 필드로 본다.
 * [RequestContextFilter] 바로 다음에 돌아 같은 requestId 를 쓴다. `http.access` 의 가벼운 한 줄은 그대로 두고 여기에 더한다.
 * - 헤더: authorization, cookie, x-internal-token, x-auth-* 값은 `***` 로 가린다.
 * - 본문: JSON·폼(urlencoded)만 문자열로 남기고 [ApiLoggingProperties.maxBodyBytes] 를 넘으면 자른다.
 *   multipart 는 `<multipart>`, 그 밖의 형식은 `<content-type>` 으로만 적는다.
 * 본문을 읽기 위해 요청·응답을 캐시 래퍼로 감싼다. 응답 본문은 finally 에서 반드시 되돌려 쓴다(copyBodyToResponse).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnProperty(prefix = "modu.logging.api", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class ApiLoggingFilter(
    private val props: ApiLoggingProperties,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean = !isLogged(request.requestURI)

    /** `/api-` 아래이면서 [ApiLoggingProperties.excludePaths] 어느 것으로도 시작하지 않는 경로만 남긴다. */
    fun isLogged(path: String): Boolean = isApiPath(path) && props.excludePaths.none { path.startsWith(it) }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        // Spring Framework 7 은 캐시 한도를 요구한다 — 본문 전체를 담는다.
        val cachedRequest = ContentCachingRequestWrapper(request, Int.MAX_VALUE)
        val cachedResponse = ContentCachingResponseWrapper(response)
        val started = System.nanoTime()
        try {
            chain.doFilter(cachedRequest, cachedResponse)
        } finally {
            val durationMs = (System.nanoTime() - started) / 1_000_000
            try {
                log(cachedRequest, cachedResponse, durationMs)
            } finally {
                cachedResponse.copyBodyToResponse()
            }
        }
    }

    private fun log(
        request: ContentCachingRequestWrapper,
        response: ContentCachingResponseWrapper,
        durationMs: Long,
    ) {
        val requestCharset = charsetOf(request.characterEncoding)
        val responseCharset = charsetOf(response.characterEncoding)
        val args =
            listOfNotNull(
                v("method", request.method),
                v("path", request.requestURI),
                v("status", response.status),
                v("durationMs", durationMs),
                kv("event", "api.access"),
                request.queryString?.let { kv("query", it) },
                kv("requestHeaders", maskedHeaders(request)),
                bodyText(request.contentType, request.contentAsByteArray, requestCharset, props.maxBodyBytes)
                    ?.let { kv("requestBody", it) },
                response.contentType?.let { kv("responseContentType", it) },
                bodyText(response.contentType, response.contentAsByteArray, responseCharset, props.maxBodyBytes)
                    ?.let { kv("responseBody", it) },
            )
        // v(): 메시지에는 값만, JSON 에는 필드로. kv() 는 JSON 필드로만 들어간다(메시지 자리에 없다). 없는 값은 필드를 내지 않는다.
        apiLog.info("{} {} {} {}ms", *args.toTypedArray())
    }

    companion object {
        const val API_PREFIX = "/api-"
        const val MASK = "***"
        private val MASKED_HEADERS = setOf("authorization", "cookie", "x-internal-token")
        private const val MASKED_HEADER_PREFIX = "x-auth-"
        private val apiLog = LoggerFactory.getLogger("api.access")

        fun isApiPath(path: String): Boolean = path.startsWith(API_PREFIX)

        /** 헤더 이름은 소문자로, 민감한 값은 가린다. 같은 이름이 여럿이면 쉼표로 잇는다. */
        fun maskedHeaders(request: HttpServletRequest): Map<String, String> {
            val headers = TreeMap<String, String>()
            for (name in request.headerNames.asSequence()) {
                val key = name.lowercase()
                headers[key] = if (isMaskedHeader(key)) MASK else request.getHeaders(name).asSequence().joinToString(",")
            }
            return headers
        }

        fun isMaskedHeader(lowerCaseName: String): Boolean =
            lowerCaseName in MASKED_HEADERS || lowerCaseName.startsWith(MASKED_HEADER_PREFIX)

        /**
         * 본문을 로그용 문자열로. JSON·폼만 내용을 남기고 [maxBodyBytes] 를 넘으면 앞부분만 두고 `…[truncated N bytes]` 를 붙인다.
         * 본문도 content-type 도 없으면 null(필드 생략).
         */
        fun bodyText(contentType: String?, bytes: ByteArray, charset: Charset, maxBodyBytes: Int): String? {
            if (contentType.isNullOrBlank()) return if (bytes.isEmpty()) null else "<unknown>"
            val mediaType = runCatching { MediaType.parseMediaType(contentType) }.getOrNull() ?: return "<$contentType>"
            return when {
                mediaType.type == "multipart" -> "<multipart>"
                isTextual(mediaType) -> truncate(bytes, charset, maxBodyBytes)
                else -> "<${mediaType.type}/${mediaType.subtype}>"
            }
        }

        private fun isTextual(mediaType: MediaType): Boolean =
            mediaType.isCompatibleWith(MediaType.APPLICATION_JSON) ||
                mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED) ||
                (mediaType.type == "application" && mediaType.subtype.endsWith("+json"))

        fun truncate(bytes: ByteArray, charset: Charset, maxBodyBytes: Int): String {
            if (bytes.size <= maxBodyBytes) return String(bytes, charset)
            val kept = String(bytes, 0, maxBodyBytes, charset)
            return "$kept…[truncated ${bytes.size - maxBodyBytes} bytes]"
        }

        private fun charsetOf(name: String?): Charset =
            name?.let { runCatching { Charset.forName(it) }.getOrNull() } ?: StandardCharsets.UTF_8
    }
}
