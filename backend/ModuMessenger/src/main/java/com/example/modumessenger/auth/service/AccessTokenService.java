package com.example.modumessenger.auth.service;

import com.example.modumessenger.auth.JwtTokenProvider;
import com.example.modumessenger.auth.repository.RefreshTokenRedisRepository;
import com.example.modumessenger.common.exception.CustomException;
import com.example.modumessenger.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccessTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    public void checkToken(String accessToken) {
        if(!jwtTokenProvider.validateToken(accessToken)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_TOKEN_ERROR, accessToken);
        }
    }
}
