package com.example.authservice.security.handler;

import com.example.authservice.auth.entity.RefreshToken;
import com.example.authservice.auth.jwt.JwtTokenProvider;
import com.example.authservice.security.CustomAuthenticationToken;
import com.example.authservice.service.RefreshTokenService;
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
        CustomAuthenticationToken customAuthenticationToken = (CustomAuthenticationToken) authentication;

        String userId = customAuthenticationToken.getUserId();
        String email = customAuthenticationToken.getEmail();
        List<String> roles = customAuthenticationToken.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String accessToken = jwtTokenProvider.createJwtAccessToken(userId, roles);
        String refreshToken = jwtTokenProvider.createJwtRefreshToken(roles);

        RefreshToken token = RefreshToken.createToken(userId, refreshToken);

        refreshTokenService.updateRefreshToken(token);

        response.addHeader("refresh-token", refreshToken);
        response.addHeader("access-token", accessToken);
        response.addHeader("userId", userId);
    }
}
