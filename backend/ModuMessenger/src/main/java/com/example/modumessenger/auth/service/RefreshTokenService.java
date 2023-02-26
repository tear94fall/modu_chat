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

    public TokenResponseDto reissueToken(String accessToken, String refreshToken) {
        accessToken = accessToken.replace("Bearer ", "");
        refreshToken = refreshToken.replace("Bearer ", "");

        String userId = jwtTokenProvider.findUserIdByJwt(accessToken);

        RefreshToken findToken = refreshTokenRedisRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USERID_NOT_FOUND, userId));

        if(!refreshToken.equals(findToken.getToken())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_TOKEN_ERROR, refreshToken);
        }

        List<String> roles = jwtTokenProvider.getAuthentication(accessToken);

        String newAccessToken = jwtTokenProvider.createJwtAccessToken(userId, roles);
        String newRefreshToken = jwtTokenProvider.createJwtRefreshToken(roles);

        RefreshToken newToken = RefreshToken.createToken(userId, newRefreshToken);
        updateRefreshToken(newToken);

        return new TokenResponseDto(newAccessToken, newRefreshToken);
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
