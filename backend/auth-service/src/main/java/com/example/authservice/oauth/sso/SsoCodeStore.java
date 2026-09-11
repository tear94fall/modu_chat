package com.example.authservice.oauth.sso;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** oauth2:sso:{code} → SsoCode JSON. consume 은 GETDEL 이라 한 번만 쓰인다. */
@Component
@RequiredArgsConstructor
public class SsoCodeStore {

    private static final String PREFIX = "oauth2:sso:";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    public void save(String code, SsoCode value, Duration ttl) {
        try {
            redis.opsForValue().set(PREFIX + code, mapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            throw new IllegalStateException("SSO 코드 저장 실패", e);
        }
    }

    public Optional<SsoCode> consume(String code) {
        String json = redis.opsForValue().getAndDelete(PREFIX + code);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapper.readValue(json, SsoCode.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
