package com.example.wsservice.member.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("memberFeignClient")
public interface MemberFeignClient {

    @GetMapping(value = "/member/{userId}")
    String getMember(@PathVariable("userId") String userId);
}
