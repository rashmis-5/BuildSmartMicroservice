package com.company.notification.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Validates JWTs issued by the auth service.
 * Expected claims:
 *   sub          -> userId (numeric string)
 *   role         -> e.g. "PROJECT_MANAGER"
 *   departmentId -> numeric, optional
 *
 * In production: prefer asymmetric keys (RS256) + JWKS endpoint.
 * Symmetric is shown here for simplicity.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.issuer:auth-service}")
    private String issuer;

    private SecretKey key;

    @PostConstruct
    void init() {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 256 bits (32 bytes).");
        }
        this.key = new SecretKeySpec(keyBytes, "HmacSHA256");
        log.info("JWT verifier initialised. Expected issuer: {}", issuer);
    }

    public AuthenticatedUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = Long.valueOf(claims.getSubject());
            String role = claims.get("role", String.class);
            Number deptNum = claims.get("departmentId", Number.class);
            Long departmentId = deptNum == null ? null : deptNum.longValue();

            if (role == null || role.isBlank()) {
                throw new JwtException("Missing 'role' claim");
            }
            return new AuthenticatedUser(userId, role, departmentId);

        } catch (JwtException | NumberFormatException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            throw ex instanceof JwtException je
                    ? je
                    : new JwtException("Invalid token", ex);
        }
    }
}
