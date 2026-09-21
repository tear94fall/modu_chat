package com.example.pushservice.fcm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** 한 사람(userId)에게만 보내는 푸시. 등록된 토큰이 없으면 조용히 지나간다. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FcmUserMessageDto {
    private String userId;
    private String title;
    private String body;
    private Map<String, String> data;
}
