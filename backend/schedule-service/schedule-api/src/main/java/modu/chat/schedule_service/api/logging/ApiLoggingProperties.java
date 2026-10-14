package modu.chat.schedule_service.api.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * API 요청·응답 전체 로그 설정({@code modu.logging.api}).
 * {@code enabled} 를 끄면 필터 자체를 등록하지 않는다. {@code maxBodyBytes} 를 넘는 본문은 잘라서 남긴다(기본 4096, application.yml 이 올린다).
 * {@code excludePaths} 로 시작하는 경로는 남기지 않는다 — 파일 본문을 흘려보내는 끝점처럼 응답을 메모리에 담으면 안 되는 곳(http.access 는 남는다).
 */
@ConfigurationProperties("modu.logging.api")
public record ApiLoggingProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("4096") int maxBodyBytes,
        @DefaultValue List<String> excludePaths) {

    public static final int DEFAULT_MAX_BODY_BYTES = 4096;
}
