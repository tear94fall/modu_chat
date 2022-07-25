package com.example.modumessenger.fcm.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class RequestPushMessage {
    String title;
    String body;
    Map<String, String> data;
    String image;

    @Builder
    public RequestPushMessage(String title, String body, Map<String, String> data, String image) {
        this.title = title;
        this.body = body;
        this.data = data;
        this.image = image;
    }
}