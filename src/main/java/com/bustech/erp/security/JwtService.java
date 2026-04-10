package com.bustech.erp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private static final int MIN_KEY_LENGTH_BYTES = 32;

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @PostConstruct
    void validateSecretKey() {
        if (secretKey == null || secretKey.isBlank()) {
            log.error("JWT_SECRET nao esta configurado. Defina a variavel de ambiente JWT_SECRET com no minimo {} caracteres.", MIN_KEY_LENGTH_BYTES);
            throw new IllegalStateException(
                    "JWT_SECRET nao esta configurado. Defina a variavel de ambiente JWT_SECRET com no minimo "
                    + MIN_KEY_LENGTH_BYTES + " caracteres.");
        }
        if (secretKey.getBytes(StandardCharsets.UTF_8).length < MIN_KEY_LENGTH_BYTES) {
            log.error("JWT_SECRET muito curto ({} bytes). Minimo necessario: {} bytes.",
                    secretKey.getBytes(StandardCharsets.UTF_8).length, MIN_KEY_LENGTH_BYTES);
            throw new IllegalStateException(
                    "JWT_SECRET muito curto. Minimo necessario: " + MIN_KEY_LENGTH_BYTES + " bytes. "
                    + "Gere com: openssl rand -base64 64");
        }
        log.info("JWT configurado com sucesso (key={} bytes, expiration={}ms).",
                secretKey.getBytes(StandardCharsets.UTF_8).length, expirationMs);
    }

    public String generateToken(UserDetails userDetails, Long companyId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("companyId", companyId);
        return buildToken(claims, userDetails.getUsername());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractCompanyId(String token) {
        return extractClaim(token, claims -> claims.get("companyId", Long.class));
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject) {
        return Jwts.builder()
            .claims(extraClaims)
            .subject(subject)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException e) {
            log.warn("JWT parsing failed: {}", e.getMessage());
            throw e;
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
