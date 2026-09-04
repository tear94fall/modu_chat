package com.example.pushservice.fcm.service;

import com.example.pushservice.fcm.dto.FcmMessageDto;
import com.example.pushservice.fcm.dto.RequestPushMessage;
import com.example.pushservice.fcm.entity.FcmToken;
import com.example.pushservice.fcm.repository.FcmRepository;
import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmRepository fcmRepository;

    /**
     * userId 당 한 행만 유지되도록 upsert 한다(과거에는 로그인마다 새 행을 insert 해서 중복이 쌓였다).
     * 기존 행이 있으면 토큰만 갱신하고, 혹시 남아있는 다른 중복 행은 이번에 정리한다.
     */
    @Transactional
    public FcmToken saveFcmToken(FcmToken fcmToken) {
        FcmToken saved = fcmRepository.findFirstByUserIdOrderByIdDesc(fcmToken.getUserId())
                .map(existing -> {
                    existing.setFcmToken(fcmToken.getFcmToken());
                    return fcmRepository.save(existing);
                })
                .orElseGet(() -> fcmRepository.save(fcmToken));
        fcmRepository.deleteByUserIdAndIdNot(fcmToken.getUserId(), saved.getId());
        return saved;
    }

    public FcmToken searchFcmToken(String userId) {
        return fcmRepository.findFirstByUserIdOrderByIdDesc(userId).orElse(null);
    }

    public List<FcmToken> searchAllFcmToken() {
        return fcmRepository.findAll();
    }

    public void sendTargetMessage(String targetToken, String title, String body) throws FirebaseMessagingException {
        this.sendTargetMessage(targetToken, title, body, null);
    }

    public void sendTargetMessage(String targetToken, String title, String body, String image) throws FirebaseMessagingException {
        Notification notification = Notification.builder().setTitle(title).setBody(body).setImage(image).build();
        Message msg = Message.builder().setToken(targetToken).setNotification(notification).build();
        sendMessage(msg);
    }

    public void sendTopicMessage(String topic, String title, String body) throws FirebaseMessagingException {
        this.sendTopicMessage(topic, title, body, null);
    }

    public void sendTopicMessage(String topic, String title, String body, String image) throws FirebaseMessagingException {
        Notification notification = Notification.builder().setTitle(title).setBody(body).setImage(image).build();
        Message msg = Message.builder().setTopic(topic).setNotification(notification).build();
        sendMessage(msg);
    }

    public void sendTopicMessageWithData(FcmMessageDto fcmMessageDto) throws FirebaseMessagingException {
        Notification notification = Notification.builder().setTitle(fcmMessageDto.getTitle()).setBody(fcmMessageDto.getBody()).setImage(fcmMessageDto.getImage()).build();
        Message message = Message.builder()
                .setTopic(fcmMessageDto.getTopic())
                .putData("type", String.valueOf(fcmMessageDto.getType()))
                .putData("title", fcmMessageDto.getTitle())
                .putData("message", fcmMessageDto.getBody())
                .putAllData(fcmMessageDto.getData())
                .build();
        Message msg = Message.builder().setTopic(fcmMessageDto.getTopic()).setNotification(notification).build();
        sendMessage(message);
    }

    public void sendMessage(Message message) throws FirebaseMessagingException {
        FirebaseMessaging.getInstance().send(message);
    }

    public void sendAsyncMessage(Message message) throws FirebaseMessagingException {
        ApiFuture<String> stringApiFuture = FirebaseMessaging.getInstance().sendAsync(message, false);
        log.info(stringApiFuture.toString());
    }

    public void sendMessage(MulticastMessage message) throws FirebaseMessagingException {
        // sendMulticast()는 폐기된 FCM 배치 엔드포인트(/batch)를 호출해 404가 발생하므로
        // 메시지별로 개별 요청을 보내는 sendEachForMulticast()로 대체한다.
        BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
        log.info("sendEachForMulticast 완료: successCount={}, failureCount={}",
                response.getSuccessCount(), response.getFailureCount());
    }

    /**
     * 전체 등록 토큰에 멀티캐스트. Firebase 멀티캐스트 한도(project.properties.firebase-multicast-message-size)
     * 단위로 나눠 보낸다. 보낸 그룹 수를 돌려준다. debug 와 admin 컨트롤러가 공유한다.
     */
    public int broadcast(RequestPushMessage data, Long groupSize) throws FirebaseMessagingException {
        List<FcmToken> fcmTokens = searchAllFcmToken();
        if (fcmTokens.isEmpty()) return 0;
        // 중복 행(같은 userId 가 여러 행을 갖거나, 여러 유저가 우연히 같은 토큰을 갖는 경우)이
        // 그룹 수/중복 발송에 영향을 주지 않도록 토큰 문자열 기준으로 먼저 중복 제거한다.
        List<String> uniqueTokens = new LinkedHashSet<>(
                fcmTokens.stream().map(FcmToken::getFcmToken).collect(Collectors.toList())
        ).stream().toList();
        if (uniqueTokens.isEmpty()) return 0;
        AtomicInteger counter = new AtomicInteger();
        Collection<List<String>> groups = uniqueTokens.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / groupSize)).values();
        for (List<String> group : groups) {
            Notification notification = Notification.builder().setTitle(data.getTitle()).setBody(data.getBody()).setImage(data.getImage()).build();
            MulticastMessage.Builder builder = MulticastMessage.builder();
            Optional.ofNullable(data.getData()).ifPresent(builder::putAllData);
            sendMessage(builder.setNotification(notification)
                    .addAllTokens(group)
                    .build());
        }
        return groups.size();
    }
}
