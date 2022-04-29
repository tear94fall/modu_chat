package com.example.modumessenger.auth;

import com.example.modumessenger.member.entity.Member;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;

public class CustomAuthorizationToken extends UsernamePasswordAuthenticationToken {

    private CustomAuthorizationToken(Object userId, Object email, Collection<? extends GrantedAuthority> authorities) {
        // principal - userId, Credential - email
        super(userId, email, authorities);
    }

    public static CustomAuthorizationToken getTokenFromAccountContext(Member member) {
        return new CustomAuthorizationToken(member.getUserId(), member.getEmail(), new ArrayList<>());
    }

    public String getUserId() { return (String) super.getPrincipal(); }
    public String getEmail() { return (String) super.getCredentials(); }
}
