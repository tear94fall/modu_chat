package com.example.pushservice.api.internal;

import com.example.pushservice.fcm.dto.FcmMessageDto;
import com.example.pushservice.fcm.dto.RequestPushMessage;
import com.example.pushservice.fcm.service.FcmService;
import com.google.firebase.messaging.FirebaseMessagingException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** ws-service, member-service 가 Feign 으로 부르는 API. InternalApiFilter 가 보호한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-internal/push")
public class PushInternalController {

    @Value("${project.properties.firebase-multicast-message-size}")
    Long multicastMessageSize;

    private final FcmService fcmService;

    @PostMapping("/chat")
    public ResponseEntity<Void> pushMessage(@RequestBody FcmMessageDto fcmMessageDto) throws FirebaseMessagingException {
        fcmService.sendTopicMessageWithData(fcmMessageDto);
        return ResponseEntity.ok().build();
    }

    /** 공지를 올린 서비스가 전체 발송을 맡길 때 쓴다. 백오피스의 /api-admin/push/broadcast 와 같은 동작이다. */
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Integer>> broadcast(@RequestBody RequestPushMessage data) throws FirebaseMessagingException {
        int groups = fcmService.broadcast(data, multicastMessageSize);
        return ResponseEntity.ok(Map.of("groups", groups));
    }
}
