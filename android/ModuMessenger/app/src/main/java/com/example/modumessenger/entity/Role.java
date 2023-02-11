package com.example.modumessenger.entity;

public enum Role {
    ROLE_ADMIN("ROLE_ADMIN"),
    ROLE_MEMBER("ROLE_USER");

    final private String roleName;

    Role(String roleName) {
        this.roleName = roleName;
    }
}
