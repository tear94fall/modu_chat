package com.example.authservice.security.provider;

import com.example.authservice.exception.CustomException;
import com.example.authservice.exception.ErrorCode;
import com.example.authservice.member.dto.MemberDto;
import com.example.authservice.member.dto.Role;
import com.example.authservice.member.service.MemberService;
import com.example.authservice.security.CustomAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

public class CustomAuthenticationProvider implements AuthenticationProvider {

    @Autowired private MemberService memberService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        CustomAuthenticationToken token = (CustomAuthenticationToken) authentication;
        String userId = token.getUserId();
        String email = token.getEmail();

        MemberDto member = memberService.getMember(userId);

        if(!email.equals(member.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_NOT_FOUND, email);
        }

        // 관리자 계정은 비밀번호 없는 앱 로그인으로 토큰을 받을 수 없다. 관리자는 /api-public/admin/login 만 쓴다.
        if (member.getRole() == Role.ROLE_ADMIN) {
            throw new CustomException(ErrorCode.EMAIL_NOT_FOUND, email);
        }

        return CustomAuthenticationToken.getCustomAuthTokenFromMember(member);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return CustomAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private boolean CompareEmail(String email, MemberDto member) {
        return passwordEncoder.matches(email, member.getEmail());
    }
}