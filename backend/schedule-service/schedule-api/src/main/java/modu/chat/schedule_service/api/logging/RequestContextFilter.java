package modu.chat.schedule_service.api.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static net.logstash.logback.argument.StructuredArguments.v;

/**
 * 요청마다 MDC 에 requestId·userId 를 넣고(요청이 끝나면 지운다) 응답에 {@code X-Request-Id} 를 돌려준다.
 * 요청이 끝나면 {@code http.access} 로거에 접근 로그 한 줄을 남긴다(method, path, status, durationMs — JSON 필드로도 나간다).
 * 다른 필터보다 먼저 돌아야 그 필터들의 로그에도 id 가 붙는다.
 */
// 빈 이름은 Spring MVC 자동 설정의 org.springframework.web.filter.RequestContextFilter(requestContextFilter)와 겹치지 않게 따로 준다.
@Component("moduRequestContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {

    public static final String ACCESS_LOGGER = "http.access";

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger(ACCESS_LOGGER);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = RequestContext.resolve(request.getHeader(RequestContext.REQUEST_ID_HEADER));
        String userId = request.getHeader(RequestContext.USER_ID_HEADER);
        if (!StringUtils.hasText(userId)) {
            userId = null;
        }

        MDC.put(RequestContext.MDC_REQUEST_ID, requestId);
        if (userId != null) {
            MDC.put(RequestContext.MDC_USER_ID, userId);
        }
        response.setHeader(RequestContext.REQUEST_ID_HEADER, requestId);

        long start = System.nanoTime();
        // 필터 밖으로 예외가 새면 컨테이너가 500 으로 바꾼다. 그 뒤의 ERROR 디스패치는 이 필터를 다시 타지 않으므로 여기서 500 으로 적는다.
        int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        try {
            chain.doFilter(request, response);
            status = response.getStatus();
        } finally {
            String path = request.getRequestURI();
            if (!path.startsWith("/actuator")) {
                long durationMs = (System.nanoTime() - start) / 1_000_000;
                ACCESS_LOG.info("{} {} {} {}ms",
                        v("method", request.getMethod()), v("path", path), v("status", status), v("durationMs", durationMs),
                        kv("event", "http.access"));
            }
            MDC.remove(RequestContext.MDC_REQUEST_ID);
            if (userId != null) {
                MDC.remove(RequestContext.MDC_USER_ID);
            }
        }
    }
}
