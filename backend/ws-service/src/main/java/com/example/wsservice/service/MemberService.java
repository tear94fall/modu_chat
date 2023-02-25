package com.example.wsservice.service;

import com.example.wsservice.client.MemberServiceFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberServiceFeignClient memberServiceFeignClient;

    public String getMember(String userId) {
        String memberId =  memberServiceFeignClient.getMember(userId);
        return memberId;
    }
}
