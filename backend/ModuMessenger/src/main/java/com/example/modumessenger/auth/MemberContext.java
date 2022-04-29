package com.example.modumessenger.auth;

import com.example.modumessenger.member.entity.Member;
import com.example.modumessenger.member.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MemberContext extends User {

    private Member member;

    private MemberContext(Member member, String userId, String email, Collection<? extends GrantedAuthority> authorities) {
        super(userId, email, authorities);
        this.member = member;
    }

    public MemberContext(String userId, String email, String role) {
        super(userId, email, parseAuthorities(role));
    }

    public static MemberContext fromMemberModel(Member member) {
        return new MemberContext(member, member.getUserId(), member.getEmail(), new ArrayList<>());
    }

    public static List<SimpleGrantedAuthority> parseAuthorities(Role role) {
        return Stream.of(role).map(r -> new SimpleGrantedAuthority((r.getRoleName()))).collect(Collectors.toList());
    }

    public static List<SimpleGrantedAuthority> parseAuthorities(String roleName) {
        return parseAuthorities(Role.getRoleByName(roleName));
    }

    public Member getMember() {
        return this.member;
    }
}
