package com.example.memberservice.notice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/** push-service 의 RequestPushMessage 와 같은 모양. 전체 발송에 쓴다. */
@Getter
@AllArgsConstructor
public class PushMessageDto {
    private String title;
    private String body;
    private Map<String, String> data;
    private String image;
}
