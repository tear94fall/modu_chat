package com.example.authservice.service;

import com.example.authservice.auth.jwt.JwtTokenProvider;
import com.example.authservice.auth.repository.RefreshTokenRedisRepository;
import com.example.authservice.exception.CustomException;
import com.example.authservice.exception.ErrorCode;
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
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_TOKEN_ERROR, accessToken);
        }
    }
}
