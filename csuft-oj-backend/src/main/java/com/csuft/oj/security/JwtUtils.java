package com.csuft.oj.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility for creating and validating JWT access tokens.
 */
@Component
public class JwtUtils {

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtUtils(
            @Value("${csuft-oj.security.jwt.secret}") String secret,
            @Value("${csuft-oj.security.jwt.expiration-seconds}") long expirationSeconds) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    /**
     * Creates a signed JWT with user identity and role claims.
     */
    public String generateToken(Long userId, String username, String role, Integer tokenVersion) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationSeconds * 1000L);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .claim("tokenVersion", tokenVersion == null ? 0 : tokenVersion)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parses claims and verifies the token signature.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Returns true when the token is structurally valid, signed, and not expired.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Returns true when the token expiration time is before now.
     */
    public boolean isExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        }
    }

    /**
     * Restores user principal data from token claims.
     */
    public JwtUserPrincipal getPrincipal(String token) {
        Claims claims = parseClaims(token);
        Number userId = claims.get("userId", Number.class);
        String username = claims.getSubject();
        String role = claims.get("role", String.class);
        Number tokenVersion = claims.get("tokenVersion", Number.class);
        return new JwtUserPrincipal(userId.longValue(), username, role,
                tokenVersion == null ? 0 : tokenVersion.intValue());
    }

    /**
     * Token lifetime in seconds.
     */
    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
