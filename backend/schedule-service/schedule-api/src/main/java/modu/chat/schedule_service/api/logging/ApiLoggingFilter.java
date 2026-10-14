package modu.chat.schedule_service.api.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static net.logstash.logback.argument.StructuredArguments.v;

/**
 * {@code /api-} 로 시작하는 경로(api-public·api-admin·api-internal·api-debug …) 요청의 입력·출력·헤더 전부를 {@code api.access} 로거에
 * 한 줄(INFO)로 남긴다. 커머스·다른 메신저 서비스의 ApiLoggingFilter 와 같은 모양이라 OpenSearch 에서 같은 필드로 본다.
 * {@link RequestContextFilter} 바로 다음에 돌아 같은 requestId 를 쓴다. {@code http.access} 의 가벼운 한 줄은 그대로 두고 여기에 더한다.
 * <ul>
 * <li>헤더: authorization, cookie, x-internal-token, x-auth-* 값은 {@code ***} 로 가린다.</li>
 * <li>본문: JSON·폼(urlencoded)만 문자열로 남기고 {@link ApiLoggingProperties#maxBodyBytes()} 를 넘으면 자른다.
 *     multipart 는 {@code <multipart>}, 그 밖의 형식은 {@code <content-type>} 으로만 적는다.</li>
 * </ul>
 * 본문을 읽기 위해 요청·응답을 캐시 래퍼로 감싼다. 응답 본문은 finally 에서 반드시 되돌려 쓴다(copyBodyToResponse).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnProperty(prefix = "modu.logging.api", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ApiLoggingFilter extends OncePerRequestFilter {

    public static final String API_PREFIX = "/api-";
    public static final String MASK = "***";

    private static final Set<String> MASKED_HEADERS = Set.of("authorization", "cookie", "x-internal-token");
    private static final String MASKED_HEADER_PREFIX = "x-auth-";
    private static final Logger API_LOG = LoggerFactory.getLogger("api.access");

    private final ApiLoggingProperties props;

    public ApiLoggingFilter(ApiLoggingProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isLogged(request.getRequestURI());
    }

    /** {@code /api-} 아래이면서 {@link ApiLoggingProperties#excludePaths()} 어느 것으로도 시작하지 않는 경로만 남긴다. */
    public boolean isLogged(String path) {
        return isApiPath(path) && props.excludePaths().stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Spring Framework 7 은 캐시 한도를 요구한다 — 본문 전체를 담는다.
        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request, Integer.MAX_VALUE);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        long started = System.nanoTime();
        try {
            chain.doFilter(cachedRequest, cachedResponse);
        } finally {
            long durationMs = (System.nanoTime() - started) / 1_000_000;
            try {
                log(cachedRequest, cachedResponse, durationMs);
            } finally {
                cachedResponse.copyBodyToResponse();
            }
        }
    }

    private void log(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long durationMs) {
        Charset requestCharset = charsetOf(request.getCharacterEncoding());
        Charset responseCharset = charsetOf(response.getCharacterEncoding());
        List<Object> args = new ArrayList<>();
        args.add(v("method", request.getMethod()));
        args.add(v("path", request.getRequestURI()));
        args.add(v("status", response.getStatus()));
        args.add(v("durationMs", durationMs));
        args.add(kv("event", "api.access"));
        if (request.getQueryString() != null) {
            args.add(kv("query", request.getQueryString()));
        }
        args.add(kv("requestHeaders", maskedHeaders(request)));
        String requestBody = bodyText(request.getContentType(), request.getContentAsByteArray(), requestCharset, props.maxBodyBytes());
        if (requestBody != null) {
            args.add(kv("requestBody", requestBody));
        }
        if (response.getContentType() != null) {
            args.add(kv("responseContentType", response.getContentType()));
        }
        String responseBody = bodyText(response.getContentType(), response.getContentAsByteArray(), responseCharset, props.maxBodyBytes());
        if (responseBody != null) {
            args.add(kv("responseBody", responseBody));
        }
        // v(): 메시지에는 값만, JSON 에는 필드로. kv() 는 JSON 필드로만 들어간다(메시지 자리에 없다). 없는 값은 필드를 내지 않는다.
        API_LOG.info("{} {} {} {}ms", args.toArray());
    }

    public static boolean isApiPath(String path) {
        return path.startsWith(API_PREFIX);
    }

    /** 헤더 이름은 소문자로, 민감한 값은 가린다. 같은 이름이 여럿이면 쉼표로 잇는다. */
    public static Map<String, String> maskedHeaders(HttpServletRequest request) {
        Map<String, String> headers = new TreeMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            String key = name.toLowerCase();
            headers.put(key, isMaskedHeader(key) ? MASK : String.join(",", Collections.list(request.getHeaders(name))));
        }
        return headers;
    }

    public static boolean isMaskedHeader(String lowerCaseName) {
        return MASKED_HEADERS.contains(lowerCaseName) || lowerCaseName.startsWith(MASKED_HEADER_PREFIX);
    }

    /**
     * 본문을 로그용 문자열로. JSON·폼만 내용을 남기고 maxBodyBytes 를 넘으면 앞부분만 두고 {@code …[truncated N bytes]} 를 붙인다.
     * 본문도 content-type 도 없으면 null(필드 생략).
     */
    public static String bodyText(String contentType, byte[] bytes, Charset charset, int maxBodyBytes) {
        if (!StringUtils.hasText(contentType)) {
            return bytes.length == 0 ? null : "<unknown>";
        }
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException e) {
            return "<" + contentType + ">";
        }
        if ("multipart".equals(mediaType.getType())) {
            return "<multipart>";
        }
        if (isTextual(mediaType)) {
            return truncate(bytes, charset, maxBodyBytes);
        }
        return "<" + mediaType.getType() + "/" + mediaType.getSubtype() + ">";
    }

    private static boolean isTextual(MediaType mediaType) {
        return mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)
                || mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)
                || ("application".equals(mediaType.getType()) && mediaType.getSubtype().endsWith("+json"));
    }

    public static String truncate(byte[] bytes, Charset charset, int maxBodyBytes) {
        if (bytes.length <= maxBodyBytes) {
            return new String(bytes, charset);
        }
        String kept = new String(bytes, 0, maxBodyBytes, charset);
        return kept + "…[truncated " + (bytes.length - maxBodyBytes) + " bytes]";
    }

    private static Charset charsetOf(String name) {
        if (name == null) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(name);
        } catch (RuntimeException e) {
            return StandardCharsets.UTF_8;
        }
    }
}
