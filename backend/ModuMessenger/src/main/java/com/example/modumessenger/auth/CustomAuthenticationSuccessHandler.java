package com.example.modumessenger.auth;

import com.example.modumessenger.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomAuthenticationToken token = (CustomAuthenticationToken) authentication;

        String userId = token.getUserId();
        String email = token.getEmail();
        List<String> roles = token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String accessToken = jwtTokenProvider.createJwtAccessToken(email, roles);
        String refreshToken = jwtTokenProvider.createJwtRefreshToken();

        refreshTokenService.updateRefreshToken(userId, jwtTokenProvider.getRefreshToKenUUID(refreshToken));

        response.addHeader("refresh-token", refreshToken);
        response.addHeader("access-token", accessToken);
        response.addHeader("userId", userId);
    }
}
