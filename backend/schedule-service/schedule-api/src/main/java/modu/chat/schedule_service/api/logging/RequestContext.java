package modu.chat.schedule_service.api.logging;

import org.slf4j.MDC;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 로그 한 줄마다 붙는 MDC 키와 요청 id 규칙. 요청 id 는 게이트웨이(또는 호출자)가 {@code X-Request-Id} 로 넘기고,
 * 없으면 여기서 새로 만든다. 예약 작업은 회차마다 {@code job-} 접두의 새 id 를 만들어 바깥 호출에 같은 헤더로 실어 보낸다.
 */
public final class RequestContext {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String USER_ID_HEADER = "X-Auth-User-Id";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_JOB = "job";

    /** 밖에서 온 id 는 이 모양일 때만 믿는다. 아니면 새로 만든다(로그 인젝션·과도한 길이 방지). */
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private RequestContext() {
    }

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public static boolean isValid(String id) {
        return id != null && VALID_ID.matcher(id).matches();
    }

    /** 헤더 값이 유효하면 그대로, 아니면 새 id. */
    public static String resolve(String header) {
        return isValid(header) ? header : newId();
    }

    public static String currentRequestId() {
        return MDC.get(MDC_REQUEST_ID);
    }
}
