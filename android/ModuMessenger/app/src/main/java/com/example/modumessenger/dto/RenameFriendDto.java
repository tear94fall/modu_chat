package com.example.modumessenger.dto;

/** PUT /member/{userId}/friends/{friendMemberId}/name 본문 */
public class RenameFriendDto {
    private String name;

    public RenameFriendDto(String name) { this.name = name; }

    public String getName() { return name; }
}
