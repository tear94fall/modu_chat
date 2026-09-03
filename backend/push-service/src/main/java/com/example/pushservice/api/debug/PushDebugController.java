package com.example.pushservice.api.debug;

import com.example.pushservice.fcm.dto.RequestChatDto;
import com.example.pushservice.fcm.dto.RequestPushMessage;
import com.example.pushservice.fcm.entity.FcmToken;
import com.example.pushservice.fcm.service.FcmService;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 운영자용 브로드캐스트/테스트 발송. 게이트웨이 라우트가 없고 prod 에서는 빈이 생성되지 않는다.
 * 로컬/dev 에서 서비스 포트로 직접 호출한다.
 */
@Profile("!prod")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-debug/push")
public class PushDebugController {

    @Value("${project.properties.firebase-multicast-message-size}")
    Long multicastMessageSize;

    private final FcmService fcmService;

    @PostMapping("/topics/{topic}")
    public void notificationTopics(@PathVariable("topic") String topic, @RequestBody RequestPushMessage data) throws FirebaseMessagingException {
        fcmService.sendTopicMessage(topic, data.getTitle(), data.getBody(), data.getImage());
    }

    @PostMapping("/users")
    public void notificationUsers(@RequestBody RequestPushMessage data) throws FirebaseMessagingException {
        List<FcmToken> fcmTokens = fcmService.searchAllFcmToken();
        AtomicInteger counter = new AtomicInteger();
        Collection<List<FcmToken>> sendUserGroups = fcmTokens.stream().collect(Collectors.groupingBy(it -> counter.getAndIncrement() / multicastMessageSize)).values();
        for (List<FcmToken> it : sendUserGroups) {
            Notification notification = Notification.builder().setTitle(data.getTitle()).setBody(data.getBody()).setImage(data.getImage()).build();
            MulticastMessage.Builder builder = MulticastMessage.builder();
            Optional.ofNullable(data.getData()).ifPresent(builder::putAllData);
            MulticastMessage message = builder
                    .setNotification(notification)
                    .addAllTokens(it.stream().map(FcmToken::getFcmToken).collect(Collectors.toList()))
                    .build();
            fcmService.sendMessage(message);
        }
    }

    @PostMapping("/user/{userId}")
    public void notificationUser(@PathVariable("userId") String userId, @RequestBody RequestPushMessage data) throws FirebaseMessagingException {
        FcmToken fcmToken = fcmService.searchFcmToken(userId);
        fcmService.sendTargetMessage(fcmToken.getFcmToken(), data.getTitle(), data.getBody(), data.getImage());
    }

    @PostMapping("/users/{token}")
    public void notificationToken(@PathVariable("token") String token, @RequestBody RequestChatDto chatDto) throws FirebaseMessagingException {
        Notification notification = Notification.builder().setTitle(chatDto.getChatRoomName()).setBody(chatDto.getMessage()).setImage(chatDto.getImage()).build();
        Message msg = Message.builder().setToken(token).setNotification(notification).build();
        fcmService.sendMessage(msg);
    }
}
