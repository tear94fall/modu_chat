package com.example.memberservice.commerce.client

import java.time.Duration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * 회원 탈퇴 때 커머스(modu_commerce)에 고객 정리를 알린다. 커머스는 다른 저장소의 서비스라 Eureka 대신
 * `modu.commerce.url` 로 바로 부른다. Feign 을 쓰지 않는 까닭은 재시도·서킷 브레이커·config-repo 의 기본 타임아웃과
 * 섞이지 않고 짧은 타임아웃([timeout])만 걸기 위해서다. 실패하면 예외를 던지고, 삼키는 건 호출 쪽(MemberService) 몫이다.
 */
@Component
class CommerceClient(
    baseUrl: String,
    token: String,
    timeout: Duration,
) {

    @Autowired
    constructor(
        @Value("\${modu.commerce.url}") baseUrl: String,
        @Value("\${modu.internal-api.token}") token: String,
    ) : this(baseUrl, token, DEFAULT_TIMEOUT)

    private val restClient: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(timeout)
                setReadTimeout(timeout)
            },
        )
        .defaultHeader(INTERNAL_TOKEN_HEADER, token)
        .build()

    /** 커머스 고객(장바구니·쿠폰 등) 정리. 2xx 가 아니거나 연결·시간 초과면 예외. */
    fun deleteCustomer(userId: String) {
        restClient.delete()
            .uri("/api-internal/v1/customers/{userId}", userId)
            .retrieve()
            .toBodilessEntity()
    }

    companion object {
        const val INTERNAL_TOKEN_HEADER = "X-Internal-Token"
        val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(3)
    }
}
