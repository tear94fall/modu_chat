package com.example.modumessenger.auth.service;

import com.example.modumessenger.auth.entity.RefreshToken;
import com.example.modumessenger.auth.repository.RefreshTokenRedisRepository;
import com.example.modumessenger.member.entity.Member;
import com.example.modumessenger.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    public void updateRefreshToken(String userId, String uuid) {
        Member member = memberRepository.findByUserId(userId);

        refreshTokenRedisRepository.save(new RefreshToken(member.getUserId(), uuid));
    }
}
