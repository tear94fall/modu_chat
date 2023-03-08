package com.example.authservice.member.client;

import com.example.authservice.member.dto.MemberDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("member-service")
public interface MemberFeignClient {

    @GetMapping(value = "/member/id/{userId}")
    MemberDto getMember(@PathVariable("userId") String userId);
}
