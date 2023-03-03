package com.example.wsservice.member.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("chat-service")
public interface MemberServiceFeignClient {

    @GetMapping(value = "/member/{userId}")
    String getMember(@PathVariable("userId") String userId);
}
