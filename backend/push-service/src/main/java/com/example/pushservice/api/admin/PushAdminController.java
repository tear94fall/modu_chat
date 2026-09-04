package com.example.pushservice.api.admin;

import com.example.pushservice.fcm.dto.RequestPushMessage;
import com.example.pushservice.fcm.entity.FcmToken;
import com.example.pushservice.fcm.service.FcmService;
import com.google.firebase.messaging.FirebaseMessagingException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 백오피스 푸시 발송. 게이트웨이(ROLE_ADMIN JWT) 경유, InternalApiFilter 가 토큰 검사. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-admin/push")
public class PushAdminController {

    @Value("${project.properties.firebase-multicast-message-size}")
    Long multicastMessageSize;

    private final FcmService fcmService;

    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Integer>> broadcast(@RequestBody RequestPushMessage data) throws FirebaseMessagingException {
        int groups = fcmService.broadcast(data, multicastMessageSize);
        return ResponseEntity.ok(Map.of("groups", groups));
    }

    @PostMapping("/users/{userId}")
    public ResponseEntity<Void> sendToUser(@PathVariable("userId") String userId, @RequestBody RequestPushMessage data) throws FirebaseMessagingException {
        FcmToken token = fcmService.searchFcmToken(userId);
        if (token == null) {
            return ResponseEntity.notFound().build();
        }
        fcmService.sendTargetMessage(token.getFcmToken(), data.getTitle(), data.getBody(), data.getImage());
        return ResponseEntity.ok().build();
    }
}
