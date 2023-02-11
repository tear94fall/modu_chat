package com.example.modumessenger.member.entity;

import lombok.Getter;

import java.util.Arrays;
import java.util.NoSuchElementException;

@Getter
public enum Role {
    ROLE_ADMIN("ROLE_ADMIN"),
    ROLE_MEMBER("ROLE_USER");

    final private String roleName;

    Role(String roleName) {
        this.roleName = roleName;
    }

    public boolean isCorrectName(String name) {
        return name.equalsIgnoreCase(this.roleName);
    }

    public  static Role getRoleByName(String roleName) {
        return Arrays.stream(Role.values())
                .filter(r -> r.isCorrectName(roleName))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 권한 종류 입니다."));
    }
}
