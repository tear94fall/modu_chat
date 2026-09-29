package com.example.memberservice.api.dto

import com.example.memberservice.application.domain.entity.ModuService
import com.example.memberservice.application.usecase.command.BulkUsageCommand
import com.example.memberservice.application.usecase.command.RecordUsageCommand
import com.example.memberservice.application.usecase.result.ServiceUsageResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** auth-service 가 토큰을 발급한 뒤 보낸다. clientId 는 등록 클라이언트 ID. */
@Schema(description = "서비스 이용 기록 요청. 토큰 발급 한 번을 알린다.")
data class UsageRequest(
    @field:Schema(description = "토큰을 받은 회원 userId. 비었거나 회원이 아니면 기록하지 않는다.", example = "112233445566778899001")
    val userId: String? = null,
    @field:Schema(description = "토큰을 받은 OAuth 클라이언트 ID. modu-chat → CHAT, modu-commerce → COMMERCE, 그 밖은 기록하지 않는다.", example = "modu-chat")
    val clientId: String? = null,
) {
    fun toCommand() = RecordUsageCommand(userId, clientId)
}

/** 한 번에 기록을 채워 넣는다(커머스 고객 백필). usedAt 은 ISO 문자열, 없으면 지금. */
@Schema(description = "서비스 이용 기록 일괄 추가 요청(백필).")
data class BulkUsageRequest(
    @field:Schema(description = "서비스. CHAT | COMMERCE, 필수(없으면 400).", example = "COMMERCE")
    val service: ModuService? = null,
    @field:Schema(description = "회원 userId 목록. 빈 값·중복은 빼고 1000개까지(넘으면 400).", example = "[\"112233445566778899001\"]")
    val userIds: List<String>? = null,
    @field:Schema(
        description = "처음·마지막 이용 시각. ISO 문자열, 오프셋이 없으면 UTC 로 본다. 비우면 지금. 형식이 틀리면 400.",
        example = "2026-09-01T00:00:00+09:00",
    )
    val usedAt: String? = null,
) {
    fun toCommand() = BulkUsageCommand(service, userIds, usedAt)
}

data class BulkUsageResponse(val inserted: Int)

/** 백오피스 회원 상세의 서비스별 처음·마지막 이용 시각(UTC, 시간대 없음). */
data class ServiceUsageDto(
    val service: ModuService,
    val firstUsedAt: LocalDateTime,
    val lastUsedAt: LocalDateTime,
) {
    companion object {
        fun of(usage: ServiceUsageResult) = ServiceUsageDto(usage.service, usage.firstUsedAt, usage.lastUsedAt)
    }
}
