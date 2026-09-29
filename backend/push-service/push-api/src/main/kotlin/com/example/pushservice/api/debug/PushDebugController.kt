package com.example.pushservice.api.debug

import com.example.pushservice.api.dto.RequestChatDto
import com.example.pushservice.api.dto.RequestPushMessage
import com.example.pushservice.application.usecase.PushSendUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 운영자용 브로드캐스트/테스트 발송. 게이트웨이 라우트가 없고 prod 에서는 빈이 생성되지 않는다.
 * 로컬/dev 에서 서비스 포트로 직접 호출한다.
 */
@Tag(
    name = "디버그",
    description = "개발자용 테스트 발송. prod 에는 없고, 게이트웨이 라우트 없이 서비스 포트로 직접 부른다. X-Internal-Token 필요.",
)
@Profile("!prod")
@RestController
@RequestMapping("/api-debug/push")
class PushDebugController(
    private val pushSendUseCase: PushSendUseCase,
    @Value("\${project.properties.firebase-multicast-message-size}") private val multicastMessageSize: Long,
) {

    @Operation(
        summary = "주제 구독자에게 알림 발송",
        description = "FCM topic 을 구독한 기기에 알림(제목·본문·이미지)을 보낸다. 채팅방 topic 은 방 id 다. FCM 이 거절하면 500.",
    )
    @PostMapping("/topics/{topic}")
    fun notificationTopics(
        @Parameter(description = "FCM topic(채팅방이면 방 id)", example = "3f2b9c1e-8a7d-4e21-9b0a-2c6d5e4f1a3b")
        @PathVariable("topic") topic: String,
        @RequestBody data: RequestPushMessage,
    ) {
        pushSendUseCase.notifyTopic(topic, data.toCommand())
    }

    @Operation(
        summary = "전체 알림 발송",
        description = "어드민 전체 발송과 같이 등록된 모든 토큰에 500개씩 묶어 보낸다. 응답 본문은 없다.",
    )
    @PostMapping("/users")
    fun notificationUsers(@RequestBody data: RequestPushMessage) {
        pushSendUseCase.broadcast(data.toCommand(), multicastMessageSize)
    }

    @Operation(
        summary = "회원 한 명에게 알림 발송",
        description = "회원의 최신 FCM 토큰으로 알림을 보낸다. 등록된 토큰이 없으면 404, FCM 이 거절하면 500.",
    )
    @PostMapping("/user/{userId}")
    fun notificationUser(
        @Parameter(description = "받는 회원 userId(모두 계정 subject)", example = "108234567890123456789")
        @PathVariable("userId") userId: String,
        @RequestBody data: RequestPushMessage,
    ) {
        pushSendUseCase.notifyUser(userId, data.toCommand())
    }

    @Operation(
        summary = "FCM 토큰으로 직접 발송",
        description = "DB 를 거치지 않고 주어진 FCM 등록 토큰에 채팅 모양 알림(방 이름 제목, 메시지 본문)을 보낸다. FCM 이 거절하면 500.",
    )
    @PostMapping("/users/{token}")
    fun notificationToken(
        @Parameter(description = "기기의 FCM 등록 토큰", example = "dXk3...:APA91b...")
        @PathVariable("token") token: String,
        @RequestBody chatDto: RequestChatDto,
    ) {
        pushSendUseCase.notifyToken(token, chatDto.toCommand())
    }
}
