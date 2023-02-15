package com.example.modumessenger.auth.service;

import com.example.modumessenger.auth.JwtTokenProvider;
import com.example.modumessenger.auth.dto.TokenResponseDto;
import com.example.modumessenger.auth.entity.RefreshToken;
import com.example.modumessenger.auth.repository.RefreshTokenRedisRepository;
import com.example.modumessenger.common.exception.CustomException;
import com.example.modumessenger.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    public void updateRefreshToken(RefreshToken refreshToken) {
        refreshTokenRedisRepository.save(refreshToken);
    }

    public TokenResponseDto reissueToken(String token) {
        String jwt = token.replace("Bearer ", "");

        jwtTokenProvider.validateToken(jwt);

        String userId = jwtTokenProvider.findUserIdByJwt(jwt);

        RefreshToken findToken = refreshTokenRedisRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USERID_NOT_FOUND, userId));

        if(!jwt.equals(findToken.getToken())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_TOKEN_ERROR, jwt);
        }

        List<String> roles = jwtTokenProvider.getAuthentication(jwt);

        String refreshToken = jwtTokenProvider.createJwtRefreshToken(roles);
        RefreshToken newRefreshToken = RefreshToken.createToken(userId, refreshToken);
        updateRefreshToken(newRefreshToken);

        String accessToken = jwtTokenProvider.createJwtAccessToken(userId, roles);

        return new TokenResponseDto(accessToken, refreshToken);
    }

    public void logoutToken(String accessToken) {
        String jwt = accessToken.replace("Bearer ", "");

        if(!jwtTokenProvider.validateToken(jwt)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_TOKEN_ERROR, jwt);
        }

        String userId = jwtTokenProvider.findUserIdByJwt(jwt);
        refreshTokenRedisRepository.delete(refreshTokenRedisRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USERID_NOT_FOUND, userId)));
    }
}
