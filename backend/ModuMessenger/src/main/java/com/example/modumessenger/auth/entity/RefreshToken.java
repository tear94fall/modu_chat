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
    private String token;

    public RefreshToken(String userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    static public RefreshToken createToken(String userId, String token) {
        return new RefreshToken(userId, token);
    }

    public void reissue(String token) {
        this.token = token;
    }
}
