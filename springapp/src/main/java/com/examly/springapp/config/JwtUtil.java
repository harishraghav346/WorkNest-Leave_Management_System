package com.examly.springapp.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Autowired
    private Environment environment;

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    private SecretKey cachedSigningKey;

    @PostConstruct
    public void validateAndInitializeKey() {
        boolean isProd = false;
        if (environment != null && environment.getActiveProfiles() != null) {
            isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        }
        
        if (secret == null || secret.isBlank()) {
            if (isProd) {
                throw new IllegalStateException(
                    "FATAL SECURITY CONFIGURATION: Production profile 'prod' requires the 'JWT_SECRET' environment variable to be explicitly configured. Server startup aborted."
                );
            } else {
                secret = "worknest-dev-environment-only-hmac-sha256-secret-key-32bytes-min";
            }
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "FATAL SECURITY CONFIGURATION: The JWT secret key must provide at least 32 bytes (256 bits) of key material for HMAC-SHA-256 cryptographic security. Provided length: " 
                + keyBytes.length + " bytes. Server startup aborted."
            );
        }

        this.cachedSigningKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public SecretKey getSigningKey() {
        if (cachedSigningKey == null) {
            validateAndInitializeKey();
        }
        return cachedSigningKey;
    }

    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        Object role = extractAllClaims(token).get("role");
        return role != null ? role.toString() : "employee";
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
