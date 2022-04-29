package com.example.modumessenger.auth;

import com.example.modumessenger.member.entity.Member;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;

public class CustomAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public CustomAuthenticationToken(Object userId, Object email, Collection<? extends GrantedAuthority> authorities) {
        // principal - userId, Credential - email
        super(userId, email, authorities);
    }

    public static CustomAuthenticationToken getTokenFromAccountContext(Member member) {
        return new CustomAuthenticationToken(member.getUserId(), member.getEmail(), new ArrayList<>());
    }

    public String getUserId() { return (String) super.getPrincipal(); }
    public String getEmail() { return (String) super.getCredentials(); }
}
