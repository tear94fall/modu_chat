package com.example.pushservice.application.usecase

import com.example.pushservice.application.common.exception.CustomException
import com.example.pushservice.application.common.exception.ErrorCode
import com.example.pushservice.application.push.PushNotification
import com.example.pushservice.application.push.PushSender
import com.example.pushservice.application.service.FcmTokenQueryService
import com.example.pushservice.application.usecase.command.NotificationCommand
import com.example.pushservice.application.usecase.command.TopicDataPushCommand
import com.example.pushservice.application.usecase.command.UserDataPushCommand
import org.springframework.stereotype.Component

/** 푸시 발송. 토큰은 DB 에서 읽고(트랜잭션은 거기서 끝난다) FCM 호출은 트랜잭션 밖에서 한다. */
@Component
class PushSendUseCase(
    private val fcmTokenQueryService: FcmTokenQueryService,
    private val pushSender: PushSender,
) {

    /** 채팅 푸시. 데이터 전용 메시지라 앱이 알림 제목·본문을 직접 만든다(notification 은 붙이지 않는다). */
    fun sendTopicData(command: TopicDataPushCommand) {
        val data = linkedMapOf(
            "type" to command.type.toString(),
            "title" to (command.title ?: ""),
            "message" to (command.body ?: ""),
        )
        command.data?.let { data.putAll(it) }
        pushSender.sendToTopic(command.topic, null, data)
    }

    /**
     * userId 한 명에게 데이터 푸시. 토큰이 없으면(로그아웃·탈퇴) 보낼 곳이 없으니 false.
     * 채팅 푸시와 같은 data 키(title/message + 호출자 data)를 실어 앱의 같은 코드가 띄운다.
     */
    fun sendUserData(command: UserDataPushCommand): Boolean {
        val token = command.userId?.let { fcmTokenQueryService.searchFcmToken(it) } ?: return false
        val target = token.fcmToken?.takeIf { it.isNotBlank() } ?: return false
        val data = linkedMapOf("title" to (command.title ?: ""), "message" to (command.body ?: ""))
        command.data?.let { data.putAll(it) }
        pushSender.sendToToken(target, null, data)
        return true
    }

    /**
     * 전체 등록 토큰에 멀티캐스트. Firebase 멀티캐스트 한도(project.properties.firebase-multicast-message-size)
     * 단위로 나눠 보낸다. 보낸 그룹 수를 돌려준다. 내부·어드민·디버그 API 가 함께 쓴다.
     */
    fun broadcast(command: NotificationCommand, groupSize: Long): Int {
        // 중복 행(같은 userId 가 여러 행을 갖거나, 여러 유저가 우연히 같은 토큰을 갖는 경우)이
        // 그룹 수/중복 발송에 영향을 주지 않도록 토큰 문자열 기준으로 먼저 중복 제거한다.
        val uniqueTokens = fcmTokenQueryService.searchAllFcmToken().mapNotNull { it.fcmToken }.distinct()
        if (uniqueTokens.isEmpty()) return 0
        val groups = uniqueTokens.chunked(groupSize.toInt())
        val notification = PushNotification(command.title, command.body, command.image)
        for (group in groups) {
            pushSender.sendToTokens(group, notification, command.data ?: emptyMap())
        }
        return groups.size
    }

    /** 회원의 최신 토큰으로 알림(제목·본문·이미지). 등록된 토큰이 없으면 404. */
    fun notifyUser(userId: String, command: NotificationCommand) {
        val token = fcmTokenQueryService.searchFcmToken(userId) ?: throw CustomException(ErrorCode.FCM_TOKEN_NOT_FOUND, userId)
        pushSender.sendToToken(token.fcmToken, PushNotification(command.title, command.body, command.image))
    }

    /** topic 구독자에게 알림(제목·본문·이미지). */
    fun notifyTopic(topic: String, command: NotificationCommand) {
        pushSender.sendToTopic(topic, PushNotification(command.title, command.body, command.image))
    }

    /** DB 를 거치지 않고 주어진 FCM 토큰에 알림을 보낸다(디버그). */
    fun notifyToken(token: String, command: NotificationCommand) {
        pushSender.sendToToken(token, PushNotification(command.title, command.body, command.image))
    }
}
