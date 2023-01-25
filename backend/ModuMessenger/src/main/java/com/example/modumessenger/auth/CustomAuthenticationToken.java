package com.example.modumessenger.auth;

import com.example.modumessenger.member.dto.RequestLoginDto;
import com.example.modumessenger.member.entity.Member;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;

public class CustomAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public CustomAuthenticationToken(RequestLoginDto requestLoginDto) {
        super(requestLoginDto.getUserId(), requestLoginDto.getEmail());
    }

    public CustomAuthenticationToken(Object userId, Object email, Collection<? extends GrantedAuthority> authorities) {
        super(userId, email, authorities); // principal, credentials, authorities
    }

    public static CustomAuthenticationToken getCustomAuthTokenFromMember(Member member) {
        return new CustomAuthenticationToken(member.getUserId(), member.getEmail(), new ArrayList<>());
    }

    public String getUserId() { return (String) super.getPrincipal(); }
    public String getEmail() { return (String) super.getCredentials(); }
    public Collection<GrantedAuthority> getAuthorities() { return super.getAuthorities(); }
}
