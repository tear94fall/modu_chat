package com.example.memberservice.usage.dto

import com.example.memberservice.usage.MemberServiceUsage
import com.example.memberservice.usage.ModuService
import java.time.LocalDateTime

/** auth-service 가 토큰을 발급한 뒤 보낸다. clientId 는 등록 클라이언트 ID. */
data class UsageRequest(
    val userId: String? = null,
    val clientId: String? = null,
)

/** 한 번에 기록을 채워 넣는다(커머스 고객 백필). usedAt 은 ISO 문자열, 없으면 지금. */
data class BulkUsageRequest(
    val service: ModuService? = null,
    val userIds: List<String>? = null,
    val usedAt: String? = null,
)

data class BulkUsageResponse(val inserted: Int)

/** 백오피스 회원 상세의 서비스별 처음·마지막 이용 시각(UTC, 시간대 없음). */
data class ServiceUsageDto(
    val service: ModuService,
    val firstUsedAt: LocalDateTime,
    val lastUsedAt: LocalDateTime,
) {
    companion object {
        @JvmStatic
        fun from(usage: MemberServiceUsage) = ServiceUsageDto(usage.service, usage.firstUsedAt, usage.lastUsedAt)
    }
}
