package com.example.pushservice.fcm.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Data
@RequiredArgsConstructor
public class FcmMessageDto {
    private String topic;
    private int type;
    private String title;
    private String body;
    private String image;
    private Map<String, String> data;
}
