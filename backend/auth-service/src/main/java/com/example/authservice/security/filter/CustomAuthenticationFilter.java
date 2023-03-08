package com.example.authservice.security.filter;

import com.example.authservice.security.CustomAuthenticationToken;
import com.example.authservice.security.dto.RequestLoginDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CustomAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final AuthenticationFailureHandler authenticationFailureHandler;

    public CustomAuthenticationFilter(AuthenticationManager authenticationManager,
                                      AuthenticationSuccessHandler authenticationSuccessHandler, AuthenticationFailureHandler authenticationFailureHandler) {
        super("/login", authenticationManager);
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        try {
            RequestLoginDto requestLoginDto = new ObjectMapper().readValue(request.getReader(), RequestLoginDto.class);
            CustomAuthenticationToken customAuthenticationToken = new CustomAuthenticationToken(requestLoginDto);

            return super.getAuthenticationManager().authenticate(customAuthenticationToken);
        } catch (IOException e) {
            throw new BadCredentialsException(e.getMessage());
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        this.authenticationSuccessHandler.onAuthenticationSuccess(request, response, authResult);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        this.authenticationFailureHandler.onAuthenticationFailure(request, response, failed);
    }
}
