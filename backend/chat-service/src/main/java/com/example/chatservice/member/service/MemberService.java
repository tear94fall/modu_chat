package com.example.chatservice.member.service;

import com.example.chatservice.member.client.MemberFeignClient;
import com.example.chatservice.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberFeignClient memberFeignClient;

    public Member getMember(String userId) {
        return memberFeignClient.getMember(userId);
    }
}

