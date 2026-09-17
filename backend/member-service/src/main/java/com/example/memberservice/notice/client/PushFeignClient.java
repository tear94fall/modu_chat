package com.example.memberservice.notice.client;

import com.example.memberservice.notice.dto.PushMessageDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient("push-service")
public interface PushFeignClient {

    @PostMapping("/api-internal/push/broadcast")
    ResponseEntity<Map<String, Integer>> broadcast(@RequestBody PushMessageDto message);
}
