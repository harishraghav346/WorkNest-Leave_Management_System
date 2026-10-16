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
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    public static final String DEFAULT_DEV_SECRET = "worknest-dev-environment-only-hmac-sha256-secret-key-32bytes-min";

    @Autowired
    private Environment environment;

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration = 86400000L;

    private SecretKey cachedSigningKey;

    @PostConstruct
    public void validateAndInitializeKey() {
        boolean isProd = isProductionProfile();

        if (secret == null || secret.isBlank()) {
            if (isProd) {
                String prodProfile = getActiveProductionProfileName();
                throw new IllegalStateException(
                    "FATAL SECURITY CONFIGURATION: Production profile '" + prodProfile + "' requires the 'JWT_SECRET' environment variable to be explicitly configured. Server startup aborted."
                );
            } else {
                secret = DEFAULT_DEV_SECRET;
            }
        }

        if (isProd && (DEFAULT_DEV_SECRET.equals(secret.trim()) || "dev-secret-key-for-local-testing-only-replace-in-production-12345".equals(secret.trim()))) {
            throw new IllegalStateException(
                "FATAL SECURITY CONFIGURATION: The default development JWT secret cannot be used in production. A unique, securely generated secret of at least 32 bytes must be provided via JWT_SECRET."
            );
        }

        byte[] keyBytes;
        String trimmedSecret = secret.trim();

        // Support explicit Base64-encoded key material when prefixed with "base64:"
        if (trimmedSecret.startsWith("base64:")) {
            String b64Payload = trimmedSecret.substring(7).trim();
            try {
                keyBytes = Base64.getDecoder().decode(b64Payload);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                    "FATAL SECURITY CONFIGURATION: The JWT_SECRET provided with 'base64:' prefix contains invalid Base64 encoding. Failed to decode Base64 key material. Server startup aborted."
                );
            }
        } else {
            // Standard raw UTF-8 key material interpretation
            keyBytes = trimmedSecret.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "FATAL SECURITY CONFIGURATION: The JWT secret key must provide at least 32 bytes (256 bits) of key material for HMAC-SHA-256 cryptographic security. Provided length: " 
                + keyBytes.length + " bytes. Server startup aborted."
            );
        }

        this.cachedSigningKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private boolean isProductionProfile() {
        if (environment == null || environment.getActiveProfiles() == null) {
            return false;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));
    }

    private String getActiveProductionProfileName() {
        if (environment != null && environment.getActiveProfiles() != null) {
            for (String p : environment.getActiveProfiles()) {
                if (p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production")) {
                    return p;
                }
            }
        }
        return "prod";
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
