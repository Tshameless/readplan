package com.readplan.security;

import com.readplan.common.security.CurrentUser;
import com.readplan.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateToken(CurrentUser currentUser) {
        Instant now = Instant.now();
        Instant expireAt = now.plus(jwtProperties.getJwtExpireMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
            .subject(currentUser.username())
            .claim("uid", currentUser.id())
            .claim("nickname", currentUser.nickname())
            .claim("roles", currentUser.roles())
            .claim("permissions", currentUser.permissions())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expireAt))
            .signWith(getSigningKey())
            .compact();
    }

    public CurrentUser parseToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();

        return new CurrentUser(
            claims.get("uid", Long.class),
            claims.getSubject(),
            claims.get("nickname", String.class),
            toStringList(claims.get("roles", List.class)),
            toStringList(claims.get("permissions", List.class))
        );
    }

    private List<String> toStringList(List<?> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
            .filter(Objects::nonNull)
            .map(String::valueOf)
            .toList();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            try {
                keyBytes = java.security.MessageDigest.getInstance("SHA-256").digest(keyBytes);
            } catch (java.security.NoSuchAlgorithmException exception) {
                throw new IllegalStateException("SHA-256 algorithm not available", exception);
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
