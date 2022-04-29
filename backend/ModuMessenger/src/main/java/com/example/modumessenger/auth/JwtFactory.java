package com.example.modumessenger.auth;

import com.example.modumessenger.auth.exception.TokenExpiredException;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtFactory {

    private static Logger log = LoggerFactory.getLogger(JwtFactory.class);

    @Value("${security.jwt.token.secret-key:1234}")
    private String secretKey;
    @Value("${security.jwt.token.expire-length:200000}")
    private long validityInMilliseconds;

    public JwtFactory(@Value("${security.jwt.token.secret-key}") String secretKey, @Value("${security.jwt.token.expire-length:200000}") long validityInMilliseconds) {
        this.secretKey = secretKey;
        this.validityInMilliseconds = validityInMilliseconds;
    }

    public String createToken(String payload) {
        Claims claims = Jwts.claims().setSubject(payload);
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        String token = null;

        try {
            token = Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(now)
                    .setExpiration(validity)
                    .signWith(SignatureAlgorithm.HS512, secretKey)
                    .compact();
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return token;
    }

    public String getPayload(String token) {
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);

            if(claims.getBody().getExpiration().before(new Date())) {
                throw new TokenExpiredException();
            }

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
