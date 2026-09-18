package com.example.memberservice.notice.client;

import com.example.memberservice.notice.dto.PushMessageDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient("push-service")
public interface PushFeignClient {

    /** 회원 탈퇴: 그 회원의 FCM 토큰을 지운다. */
    @DeleteMapping("/api-internal/push/token/{userId}")
    ResponseEntity<Void> deleteToken(@PathVariable("userId") String userId);

    @PostMapping("/api-internal/push/broadcast")
    ResponseEntity<Map<String, Integer>> broadcast(@RequestBody PushMessageDto message);
}
