package com.example.pushservice.api.internal;

import com.example.pushservice.fcm.dto.FcmMessageDto;
import com.example.pushservice.fcm.service.FcmService;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** ws-service 가 Feign 으로 부르는 API. InternalApiFilter 가 보호한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-internal/push")
public class PushInternalController {

    private final FcmService fcmService;

    @PostMapping("/chat")
    public ResponseEntity<Void> pushMessage(@RequestBody FcmMessageDto fcmMessageDto) throws FirebaseMessagingException {
        fcmService.sendTopicMessageWithData(fcmMessageDto);
        return ResponseEntity.ok().build();
    }
}
