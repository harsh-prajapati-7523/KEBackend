package com.ke.ticketsystemke.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET =
            "THIS_IS_A_VERY_SECURE_SECRET_KEY_123456789";

    private static final Key KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes());

    public static String generateToken(String employeeId) {

        return Jwts.builder()
                .setSubject(employeeId)
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(System.currentTimeMillis() + 86400000)
                )
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static String extractEmployeeId(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}