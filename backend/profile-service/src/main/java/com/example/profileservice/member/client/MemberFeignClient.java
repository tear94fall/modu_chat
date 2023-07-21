package com.example.profileservice.member.client;

import com.example.profileservice.member.dto.AddProfileDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("member-service")
public interface MemberFeignClient {

    @PostMapping("/member/profile")
    ResponseEntity<Long> addMemberProfile(@RequestBody AddProfileDto addProfileDto);
}
