package com.example.authservice.security;

import com.example.authservice.member.dto.MemberDto;
import com.example.authservice.security.dto.RequestLoginDto;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public CustomAuthenticationToken(RequestLoginDto requestLoginDto) {
        super(requestLoginDto.getUserId(), requestLoginDto.getEmail());
    }

    public CustomAuthenticationToken(Object userId, Object email, Collection<? extends GrantedAuthority> authorities) {
        super(userId, email, authorities); // principal, credentials, authorities
    }

    public static CustomAuthenticationToken getCustomAuthTokenFromMember(MemberDto member) {
        return new CustomAuthenticationToken(member.getUserId(), member.getEmail(), new ArrayList<>(List.of(new SimpleGrantedAuthority(member.getRole().getRoleName()))));
    }

    public String getUserId() { return (String) super.getPrincipal(); }
    public String getEmail() { return (String) super.getCredentials(); }
    public Collection<GrantedAuthority> getAuthorities() { return super.getAuthorities(); }
}
