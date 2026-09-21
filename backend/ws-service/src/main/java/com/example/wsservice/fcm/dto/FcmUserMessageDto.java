package com.example.wsservice.fcm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** 방(topic)이 아니라 한 사람에게만 보내는 푸시. 반응 알림은 메시지 작성자 한 명에게만 간다. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FcmUserMessageDto {
    private String userId;
    private String title;
    private String body;
    private Map<String, String> data;
}
