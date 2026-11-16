package com.cstudy.auth.service;

import com.cstudy.common.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** JWT 발급과 검증(서명·만료). 비밀키는 환경 변수 JWT_SECRET 에서만 읽는다. (QC-003) */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration validity;
    private final Clock clock;

    public JwtTokenProvider(AppProperties properties, Clock clock) {
        String secret = properties.jwt().secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET 환경 변수를 32바이트 이상으로 설정해야 합니다.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validity = Duration.ofMinutes(properties.jwt().expirationMinutes());
        this.clock = clock;
    }

    public String createToken(String adminId, String role) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(adminId)
                .claim(CLAIM_ROLE, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(key)
                .compact();
    }

    /** 서명이 맞고 만료되지 않았으면 토큰 내용을 돌려준다. 아니면 빈 값. */
    public Optional<TokenClaims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new TokenClaims(claims.getSubject(), claims.get(CLAIM_ROLE, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long validitySeconds() {
        return validity.toSeconds();
    }

    public record TokenClaims(String adminId, String role) {
    }
}
