package com.example.wsservice.fcm.service;

import com.example.wsservice.fcm.client.FcmFeignClient;
import com.example.wsservice.fcm.dto.FcmMessageDto;
import com.example.wsservice.fcm.dto.FcmUserMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmFeignClient fcmFeignClient;

    public void sendFcmMessage(FcmMessageDto fcmMessageDto) {
        fcmFeignClient.sendMessage(fcmMessageDto);
    }

    public void sendUserMessage(FcmUserMessageDto fcmUserMessageDto) {
        fcmFeignClient.sendUserMessage(fcmUserMessageDto);
    }
}
