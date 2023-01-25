package com.example.modumessenger.auth;

import com.example.modumessenger.common.exception.CustomException;
import com.example.modumessenger.common.exception.ErrorCode;
import com.example.modumessenger.member.entity.Member;
import com.example.modumessenger.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.NoSuchElementException;

public class CustomAuthenticationProvider implements AuthenticationProvider {

    @Autowired private MemberRepository memberRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        CustomAuthenticationToken token = (CustomAuthenticationToken) authentication;
        String userId = token.getUserId();
        String email = token.getEmail();

        Member member = memberRepository.searchMemberByUserId(userId).orElseThrow(() -> new NoSuchElementException("일치하는 계정이 없습니다."));

        if(!email.equals(member.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_NOT_FOUND, email);
        }

        return CustomAuthenticationToken.getCustomAuthTokenFromMember(member);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return CustomAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private boolean CompareEmail(String email, Member member) {
        return passwordEncoder.matches(email, member.getEmail());
    }

}