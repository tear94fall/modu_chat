package com.example.gatewayservice.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${security.jwt.token.secret-key}")
    private String secretKey;

    @Value("${security.jwt.token.access-expired}")
    private Long ACCESS_TOKEN_EXPIRED_TIME;

    @Value("${security.jwt.token.refresh-expired}")
    private Long REFRESH_TOKEN_EXPIRED_TIME;

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    public String createJwtAccessToken(String userId, List<String> roles) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("roles", roles);
        Date now = new Date();
        Date expiration = new Date(now.getTime() + ACCESS_TOKEN_EXPIRED_TIME);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String createJwtRefreshToken(List<String> roles) {
        Claims claims = Jwts.claims();
        claims.put("uuid", UUID.randomUUID());
        claims.put("roles", roles);
        Date now = new Date();
        Date expiration = new Date(now.getTime() + REFRESH_TOKEN_EXPIRED_TIME);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public String getRefreshToKenUUID(String refreshToken) {
        return getClaims(refreshToken).get("uuid").toString();
    }

    public String getRolesToken(String token) {
        return getClaims(token).get("roles").toString();
    }

    public String findUserIdByJwt(String token) {
        return getClaims(token).getSubject();
    }

    public List<String> getAuthentication(String token) {
        return (List<String>) getClaims(token).get("roles");
    }

    public void validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token);

        } catch (SecurityException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty.");
        } catch (NullPointerException ex) {
            log.error("JWT RefreshToken is empty");
        }
    }
}
