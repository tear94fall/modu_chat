package com.example.profileservice.api.logging

import org.slf4j.MDC
import java.util.UUID

/**
 * 로그 한 줄마다 붙는 MDC 키와 요청 id 규칙. 요청 id 는 게이트웨이(또는 호출자)가 `X-Request-Id` 로 넘기고,
 * 없으면 여기서 새로 만든다. Feign·Kafka 로 나갈 때 같은 헤더로 실어 보내 서비스 사이에서 한 요청을 따라갈 수 있다.
 */
object RequestContext {
    const val REQUEST_ID_HEADER = "X-Request-Id"
    const val USER_ID_HEADER = "X-Auth-User-Id"
    const val MDC_REQUEST_ID = "requestId"
    const val MDC_USER_ID = "userId"

    /** 밖에서 온 id 는 이 모양일 때만 믿는다. 아니면 새로 만든다(로그 인젝션·과도한 길이 방지). */
    private val VALID_ID = Regex("[A-Za-z0-9_-]{1,64}")

    fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(16)

    fun isValid(id: String?): Boolean = id != null && VALID_ID.matches(id)

    /** 헤더 값이 유효하면 그대로, 아니면 새 id. */
    fun resolve(header: String?): String = if (header != null && isValid(header)) header else newId()

    fun currentRequestId(): String? = MDC.get(MDC_REQUEST_ID)

    fun currentUserId(): String? = MDC.get(MDC_USER_ID)
}
