package com.example.modumessenger.auth;

import com.example.modumessenger.member.entity.Member;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private JwtFactory jwtFactory;
    private ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        PostAuthorizationToken postAuthorizationToken = (PostAuthorizationToken) authentication;
        MemberContext context = (MemberContext) postAuthorizationToken.getPrincipal();

        String token = jwtFactory.createToken(context);

        response.addHeader("token", token);
        response.addHeader("userId", (String) context.getMember().getUserId());
    }
}
