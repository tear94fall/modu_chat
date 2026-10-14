package com.example.pushservice.api.admin

import com.example.pushservice.api.dto.RequestPushMessage
import com.example.pushservice.application.usecase.PushSendUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 백오피스 푸시 발송. 게이트웨이(ROLE_ADMIN JWT) 경유, InternalApiFilter 가 토큰 검사. */
@Tag(
    name = "푸시 발송 (어드민)",
    description = "어드민 콘솔용. 게이트웨이가 직원 토큰(ROLE_ADMIN, aud modu-admin)을 확인하고 X-Internal-Token 을 붙인다.",
)
@RestController
@RequestMapping("/api-admin/push")
class PushAdminController(
    private val pushSendUseCase: PushSendUseCase,
    @Value("\${project.properties.firebase-multicast-message-size}") private val multicastMessageSize: Long,
) {

    @Operation(
        summary = "전체 푸시 발송",
        description = "등록된 모든 FCM 토큰(중복 제거)에 알림을 보낸다. 토큰을 500개씩 묶어 보내고 보낸 묶음 수를 {\"groups\": n} 으로 돌려준다. " +
            "토큰이 없으면 groups 0. 개별 토큰 실패는 무시하고 로그만 남긴다.",
    )
    @PostMapping("/broadcast")
    fun broadcast(@RequestBody data: RequestPushMessage): ResponseEntity<Map<String, Int>> {
        val groups = pushSendUseCase.broadcast(data.toCommand(), multicastMessageSize)
        return ResponseEntity.ok(mapOf("groups" to groups))
    }

    @Operation(
        summary = "회원 한 명에게 푸시 발송",
        description = "회원의 최신 FCM 토큰으로 알림(제목·본문·이미지)을 보낸다. data 는 쓰지 않는다. " +
            "등록된 토큰이 없으면(탈퇴 등) 404, FCM 이 거절하면 500.",
    )
    @PostMapping("/users/{userId}")
    fun sendToUser(
        @Parameter(description = "받는 회원 userId(모두 계정 subject)", example = "108234567890123456789")
        @PathVariable("userId") userId: String,
        @RequestBody data: RequestPushMessage,
    ): ResponseEntity<Void> {
        pushSendUseCase.notifyUser(userId, data.toCommand())
        return ResponseEntity.ok().build()
    }
}
