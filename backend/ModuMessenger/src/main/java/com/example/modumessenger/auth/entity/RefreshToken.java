package com.example.modumessenger.auth.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@RedisHash(value = "refresh_token")
@NoArgsConstructor
public class RefreshToken {

    @Id
    private String userId;
    private String refreshTokenUUID;

    public RefreshToken(String userId, String refreshTokenUUID) {
        this.userId = userId;
        this.refreshTokenUUID = refreshTokenUUID;
    }
}