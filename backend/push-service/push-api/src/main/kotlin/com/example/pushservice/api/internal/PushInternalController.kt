package com.example.pushservice.api.internal

import com.example.pushservice.api.dto.FcmMessageDto
import com.example.pushservice.api.dto.FcmUserMessageDto
import com.example.pushservice.api.dto.RequestPushMessage
import com.example.pushservice.application.usecase.PushSendUseCase
import com.example.pushservice.application.usecase.PushTokenUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** ws-service, member-service 가 Feign 으로 부르는 API. InternalApiFilter 가 보호한다. */
@Tag(
    name = "푸시 (내부)",
    description = "서비스끼리만 호출(ws-service·member-service 등). X-Internal-Token 필요, 게이트웨이로는 열려 있지 않다.",
)
@RestController
@RequestMapping("/api-internal/push")
class PushInternalController(
    private val pushTokenUseCase: PushTokenUseCase,
    private val pushSendUseCase: PushSendUseCase,
    @Value("\${project.properties.firebase-multicast-message-size}") private val multicastMessageSize: Long,
) {

    /** 회원 탈퇴(member-service). 그 회원의 FCM 토큰을 지운다. */
    @Operation(
        summary = "탈퇴 회원 FCM 토큰 삭제",
        description = "회원 탈퇴 때 member-service 가 부른다. 이 회원의 FCM 토큰을 모두 지우고 204 를 돌려준다. 토큰이 없어도 204.",
    )
    @DeleteMapping("/token/{userId}")
    fun deleteToken(
        @Parameter(description = "회원 userId(모두 계정 subject)", example = "108234567890123456789")
        @PathVariable("userId") userId: String,
    ): ResponseEntity<Void> {
        pushTokenUseCase.delete(userId)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "채팅방 푸시 발송",
        description = "ws-service 가 새 메시지마다 부른다. 방 id topic 을 구독한 기기에 데이터 전용 메시지(type·title·message + data)를 보낸다. " +
            "FCM 이 거절하면 500.",
    )
    @PostMapping("/chat")
    fun pushMessage(@RequestBody fcmMessageDto: FcmMessageDto): ResponseEntity<Void> {
        pushSendUseCase.sendTopicData(fcmMessageDto.toCommand())
        return ResponseEntity.ok().build()
    }

    /** ws-service 가 반응 알림처럼 한 사람에게만 보낼 때. 토큰이 없으면 204. */
    @Operation(
        summary = "회원 한 명에게 푸시 발송",
        description = "ws-service 가 반응 알림처럼 한 사람에게만 보낼 때 부른다. 회원의 최신 FCM 토큰으로 데이터 전용 메시지를 보내고 200. " +
            "userId 가 없거나 토큰이 없으면 보내지 않고 204.",
    )
    @PostMapping("/user")
    fun pushUser(@RequestBody dto: FcmUserMessageDto): ResponseEntity<Void> =
        if (pushSendUseCase.sendUserData(dto.toCommand())) ResponseEntity.ok().build() else ResponseEntity.noContent().build()

    /** 공지를 올린 서비스가 전체 발송을 맡길 때 쓴다. 백오피스의 /api-admin/push/broadcast 와 같은 동작이다. */
    @Operation(
        summary = "전체 푸시 발송",
        description = "공지를 올린 서비스가 전체 발송을 맡길 때 쓴다. 어드민 전체 발송과 같다: 등록된 모든 토큰(중복 제거)에 500개씩 묶어 보내고 " +
            "보낸 묶음 수를 {\"groups\": n} 으로 돌려준다.",
    )
    @PostMapping("/broadcast")
    fun broadcast(@RequestBody data: RequestPushMessage): ResponseEntity<Map<String, Int>> {
        val groups = pushSendUseCase.broadcast(data.toCommand(), multicastMessageSize)
        return ResponseEntity.ok(mapOf("groups" to groups))
    }
}
