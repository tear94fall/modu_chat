package com.example.pushservice.fcm.service

import com.example.pushservice.fcm.dto.FcmMessageDto
import com.example.pushservice.fcm.dto.FcmUserMessageDto
import com.example.pushservice.fcm.dto.RequestPushMessage
import com.example.pushservice.fcm.entity.FcmToken
import com.example.pushservice.fcm.repository.FcmRepository
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FcmService(private val fcmRepository: FcmRepository) {

    private val log = LoggerFactory.getLogger(FcmService::class.java)

    /**
     * userId 당 한 행만 유지되도록 upsert 한다(과거에는 로그인마다 새 행을 insert 해서 중복이 쌓였다).
     * 기존 행이 있으면 토큰만 갱신하고, 혹시 남아있는 다른 중복 행은 이번에 정리한다.
     */
    @Transactional
    fun saveFcmToken(fcmToken: FcmToken): FcmToken {
        val saved = fcmRepository.findFirstByUserIdOrderByIdDesc(fcmToken.userId)
            .map { existing ->
                existing.fcmToken = fcmToken.fcmToken
                fcmRepository.save(existing)
            }
            .orElseGet { fcmRepository.save(fcmToken) }
        fcmRepository.deleteByUserIdAndIdNot(fcmToken.userId, saved.id)
        return saved
    }

    /** 회원 탈퇴 때 member-service 가 부른다. 토큰이 없어도 조용히 지나간다. */
    @Transactional
    fun deleteFcmToken(userId: String) {
        fcmRepository.deleteAllByUserId(userId)
    }

    fun searchFcmToken(userId: String): FcmToken? = fcmRepository.findFirstByUserIdOrderByIdDesc(userId).orElse(null)

    fun searchAllFcmToken(): List<FcmToken> = fcmRepository.findAll()

    fun sendTargetMessage(targetToken: String?, title: String?, body: String?) = sendTargetMessage(targetToken, title, body, null)

    fun sendTargetMessage(targetToken: String?, title: String?, body: String?, image: String?) {
        val notification = Notification.builder().setTitle(title).setBody(body).setImage(image).build()
        sendMessage(Message.builder().setToken(targetToken).setNotification(notification).build())
    }

    fun sendTopicMessage(topic: String?, title: String?, body: String?) = sendTopicMessage(topic, title, body, null)

    fun sendTopicMessage(topic: String?, title: String?, body: String?, image: String?) {
        val notification = Notification.builder().setTitle(title).setBody(body).setImage(image).build()
        sendMessage(Message.builder().setTopic(topic).setNotification(notification).build())
    }

    /** 채팅 푸시. 데이터 전용 메시지라 앱이 알림 제목·본문을 직접 만든다(notification 은 붙이지 않는다 — 옛 자바와 같다). */
    fun sendTopicMessageWithData(fcmMessageDto: FcmMessageDto) {
        val builder = Message.builder()
            .setTopic(fcmMessageDto.topic)
            .putData("type", fcmMessageDto.type.toString())
            .putData("title", fcmMessageDto.title)
            .putData("message", fcmMessageDto.body)
        fcmMessageDto.data?.let { builder.putAllData(it) }
        sendMessage(builder.build())
    }

    /**
     * userId 한 명에게 데이터 푸시. 토큰이 없으면(로그아웃·탈퇴) 보낼 곳이 없으니 false.
     * 채팅 푸시와 같은 data 키(type/title/message + 호출자 data)를 실어 앱의 같은 코드가 띄운다.
     */
    fun sendUserMessageWithData(dto: FcmUserMessageDto): Boolean {
        val token = dto.userId?.let { searchFcmToken(it) } ?: return false
        val target = token.fcmToken?.takeIf { it.isNotBlank() } ?: return false
        val builder = Message.builder()
            .setToken(target)
            .putData("title", dto.title ?: "")
            .putData("message", dto.body ?: "")
        dto.data?.let { builder.putAllData(it) }
        sendMessage(builder.build())
        return true
    }

    fun sendMessage(message: Message) {
        FirebaseMessaging.getInstance().send(message)
    }

    fun sendAsyncMessage(message: Message) {
        val future = FirebaseMessaging.getInstance().sendAsync(message, false)
        log.info(future.toString())
    }

    fun sendMessage(message: MulticastMessage) {
        // sendMulticast()는 폐기된 FCM 배치 엔드포인트(/batch)를 호출해 404가 발생하므로
        // 메시지별로 개별 요청을 보내는 sendEachForMulticast()로 대체한다.
        val response = FirebaseMessaging.getInstance().sendEachForMulticast(message)
        log.info("sendEachForMulticast 완료: successCount={}, failureCount={}", response.successCount, response.failureCount)
    }

    /**
     * 전체 등록 토큰에 멀티캐스트. Firebase 멀티캐스트 한도(project.properties.firebase-multicast-message-size)
     * 단위로 나눠 보낸다. 보낸 그룹 수를 돌려준다. debug 와 admin 컨트롤러가 공유한다.
     */
    fun broadcast(data: RequestPushMessage, groupSize: Long): Int {
        val fcmTokens = searchAllFcmToken()
        if (fcmTokens.isEmpty()) return 0
        // 중복 행(같은 userId 가 여러 행을 갖거나, 여러 유저가 우연히 같은 토큰을 갖는 경우)이
        // 그룹 수/중복 발송에 영향을 주지 않도록 토큰 문자열 기준으로 먼저 중복 제거한다.
        val uniqueTokens = fcmTokens.mapNotNull { it.fcmToken }.distinct()
        if (uniqueTokens.isEmpty()) return 0
        val groups = uniqueTokens.chunked(groupSize.toInt())
        for (group in groups) {
            val notification = Notification.builder().setTitle(data.title).setBody(data.body).setImage(data.image).build()
            val builder = MulticastMessage.builder()
            data.data?.let { builder.putAllData(it) }
            sendMessage(builder.setNotification(notification).addAllTokens(group).build())
        }
        return groups.size
    }
}
