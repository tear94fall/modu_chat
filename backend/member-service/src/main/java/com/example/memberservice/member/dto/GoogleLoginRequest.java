package com.example.memberservice.member.dto;

import com.example.memberservice.global.lock.Lockable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class GoogleLoginRequest implements Lockable {

    private String authType;
    private String idToken;

    @Override
    public String getKey() {
        return authType + "-" + idToken;
    }
}
