package com.example.wsservice.member.service;

import com.example.wsservice.member.client.MemberFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberFeignClient memberFeignClient;

    public String getMember(String userId) {
        return memberFeignClient.getMember(userId);
    }
}
