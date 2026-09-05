package com.example.wsservice.fcm.client;

import com.example.wsservice.fcm.dto.FcmMessageDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("push-service")
public interface FcmFeignClient {

    @PostMapping("/api-internal/push/chat")
    Void sendMessage(@RequestBody FcmMessageDto fcmMessageDto);
}
