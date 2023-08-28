package com.example.modumessenger.dto;

public class AddFriendDto {

    private String email;

    public AddFriendDto(String email) {
        this.email = email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }
}
