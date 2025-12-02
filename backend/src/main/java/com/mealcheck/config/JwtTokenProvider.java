package com.mealcheck.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    // 자동 로그인(remember-me) 시 사용할 확장 만료 시간 (기본: 30일)
    @Value("${jwt.rememberExpiration:2592000000}")
    private long jwtRememberExpirationInMs;

    private SecretKey getSigningKey() {
        // jwt.secret 을 일반 텍스트로 사용 (Base64 인코딩 강제 X)
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        // JJWT 요구사항: 최소 256bit(32byte) 이상이어야 함 → 부족하면 32바이트까지 패딩
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username) {
        return generateToken(username, false);
    }

    public String generateToken(String username, boolean rememberMe) {
        Date now = new Date();
        long expiration = rememberMe ? jwtRememberExpirationInMs : jwtExpirationInMs;
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}

