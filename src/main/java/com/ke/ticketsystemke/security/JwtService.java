package com.ke.ticketsystemke.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret:${JWT_SECRET}}")
    private String secret;

    @Value("${jwt.expiration-ms:${JWT_EXPIRATION_MS:1800000}}")
    private long expirationMs;

    private static final String TOKEN_USE_CLAIM = "tokenUse";
    private static final String SESSION_HASH_CLAIM = "sessionHash";
    public static final String TOKEN_USE_ACCESS = "ACCESS";
    public static final String TOKEN_USE_PIN_SETUP = "PIN_SETUP";

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(String employeeId) {
        return generateToken(employeeId, null);
    }

    public String generateToken(String employeeId, String sessionHash) {

        return Jwts.builder()
                .setSubject(employeeId)
                .claim(TOKEN_USE_CLAIM, TOKEN_USE_ACCESS)
                .claim(SESSION_HASH_CLAIM, sessionHash)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generatePinSetupToken(String employeeId, String sessionHash) {
        return Jwts.builder()
                .setSubject(employeeId)
                .claim(TOKEN_USE_CLAIM, TOKEN_USE_PIN_SETUP)
                .claim(SESSION_HASH_CLAIM, sessionHash)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmployeeId(String token) {

        Claims claims = extractClaims(token);
        String subject = claims.getSubject();
        if (subject == null) {
            log.warn("event=jwt_parse_failed reason=no_subject correlationId={}", MDC.get("correlationId"));
        }
        return subject;
    }

    public String extractEmployeeId(Claims claims) {
        String subject = claims.getSubject();
        if (subject == null) {
            log.warn("event=jwt_parse_failed reason=no_subject correlationId={}", MDC.get("correlationId"));
        }
        return subject;
    }

    public String extractTokenUse(String token) {
        return extractClaims(token).get(TOKEN_USE_CLAIM, String.class);
    }

    public String extractTokenUse(Claims claims) {
        return claims.get(TOKEN_USE_CLAIM, String.class);
    }

    public String extractSessionHash(String token) {
        return extractClaims(token).get(SESSION_HASH_CLAIM, String.class);
    }

    public String extractSessionHash(Claims claims) {
        return claims.get(SESSION_HASH_CLAIM, String.class);
    }

    public boolean isTokenValid(String token, String employeeId) {

        String extracted = extractEmployeeId(token);

        return extracted.equals(employeeId);
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
