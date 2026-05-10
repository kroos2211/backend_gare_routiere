package com.mahta.backend_gare_routiere.security.jwt;

import com.mahta.backend_gare_routiere.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {

        this.secretKey = Keys.hmacShaKeyFor(
                "mahta_gare_routiere_super_secret_jwt_key_2026_minimum_32_chars"
                        .getBytes(StandardCharsets.UTF_8)
        );

        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(
            UUID userId,
            String email,
            UserRole role
    ) {

        return buildToken(
                userId,
                email,
                role,
                expirationMs,
                "access"
        );
    }

    public String generateRefreshToken(
            UUID userId,
            String email,
            UserRole role
    ) {

        return buildToken(
                userId,
                email,
                role,
                refreshExpirationMs,
                "refresh"
        );
    }

    private String buildToken(
            UUID userId,
            String email,
            UserRole role,
            long ttl,
            String type
    ) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttl);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {

        try {
            getClaims(token);
            return true;

        } catch (JwtException | IllegalArgumentException ex) {

            log.warn("Invalid JWT token: {}", ex.getMessage());

            return false;
        }
    }

    public boolean isRefreshToken(String token) {

        return "refresh".equals(
                getClaims(token).get("type", String.class)
        );
    }

    public boolean isAccessToken(String token) {

        return "access".equals(
                getClaims(token).get("type", String.class)
        );
    }

    public UUID getUserIdFromToken(String token) {

        return UUID.fromString(
                getClaims(token).getSubject()
        );
    }

    public String getRoleFromToken(String token) {

        return getClaims(token)
                .get("role", String.class);
    }

    public String getEmailFromToken(String token) {

        return getClaims(token)
                .get("email", String.class);
    }

    private Claims getClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}