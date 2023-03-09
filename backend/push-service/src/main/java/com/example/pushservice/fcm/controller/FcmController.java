package com.example.pushservice.fcm.controller;

import com.example.pushservice.fcm.dto.FcmMessageDto;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class FcmController {

    @Value("${project.properties.firebase-multicast-message-size}")
    Long multicastMessageSize;

    private final FcmService fcmService;

    @PostMapping("/push/chat")
    public ResponseEntity<Void> pushMessage(@RequestBody FcmMessageDto fcmMessageDto) throws FirebaseMessagingException {
        fcmService.sendTopicMessageWithData(fcmMessageDto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/chat/{userId}/token")
    public ResponseEntity<String> registerFcmToken(@PathVariable("userId") String userId, @RequestBody String token) {
        FcmToken fcmToken = new FcmToken(userId, token);
        FcmToken saveToken = fcmService.saveFcmToken(fcmToken);

        return ResponseEntity.ok().body(saveToken.getFcmToken());
    }

    @PostMapping(value = "/push/topics/{topic}")
    public void notificationTopics(@PathVariable("topic") String topic, @RequestBody RequestPushMessage data) throws FirebaseMessagingException {
        fcmService.sendTopicMessage(topic, data.getTitle(), data.getBody(), data.getImage());
    }

    @PostMapping(value = "/push/users")
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

    @PostMapping(value = "/push/user/{userId}")
    public void notificationUser(@PathVariable("userId") String userId, @RequestBody RequestPushMessage data) throws FirebaseMessagingException {
        FcmToken fcmToken = fcmService.searchFcmToken(userId);
        fcmService.sendTargetMessage(fcmToken.getFcmToken(), data.getTitle(), data.getBody(), data.getImage());
    }

    @PostMapping(value = "/push/users/{token}")
    public void notificationToken(@PathVariable("token") String token, @RequestBody RequestChatDto chatDto) throws FirebaseMessagingException {
        Notification notification = Notification.builder().setTitle(chatDto.getChatRoomName()).setBody(chatDto.getMessage()).setImage(chatDto.getImage()).build();
        Message.Builder builder = Message.builder();
        Message msg = builder.setToken(token).setNotification(notification).build();
        fcmService.sendMessage(msg);
    }
}
