package com.kapilagro.sasyak.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {

    // TODO: Replace hardcoded secret keys with application properties
    private static final String SECRET_KEY = "KA92Ab8OphARt/lQwY6u5Zn+LkwISP6m9ABjI3JQfVo=";
    private static final String REFRESH_SECRET_KEY = "HyaFfpChC8IekjRGc5loPYid4/uHekm0dBmlJaYnvq0=";

    private static final long ACCESS_TOKEN_EXPIRATION = (1000 * 60 * 30) * 3; // 90 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 24 * 7; // 7 days
    public final long RESET_TOKEN_EXPIRY = 3600000; // 1 hour (made public for consistency)

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY));
    }

    private SecretKey getRefreshSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(REFRESH_SECRET_KEY));
    }

    // Generate Access Token
    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(userDetails, getSigningKey(), ACCESS_TOKEN_EXPIRATION);
    }

    // Generate Reset Token
    public String generateResetToken(Map<String, Object> claims, long expiryMillis) {
        return Jwts.builder()
                .claims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiryMillis))
                .signWith(getSigningKey())
                .compact();
    }

    // Generate Refresh Token
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenId", UUID.randomUUID().toString());
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(getRefreshSigningKey())
                .compact();
    }

    // Common token generation logic
    private String generateToken(UserDetails userDetails, SecretKey key, long expiration) {
        Map<String, Object> claims = new HashMap<>();
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    // Extract Username from Access Token
    public String extractUsername(String token) {
        return extractClaims(token, getSigningKey()).getSubject();
    }

    // Extract Username from Refresh Token
    public String extractUsernameFromRefreshToken(String token) {
        return extractClaims(token, getRefreshSigningKey()).getSubject();
    }

    // Extract Email from Reset Token
    public String extractUsernameFromResetToken(String token) {
        return extractClaims(token, getSigningKey()).get("email", String.class);
    }

    // Validate Access Token
    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token, getSigningKey());
    }

    // Validate Refresh Token
    public boolean validateRefreshToken(String token, UserDetails userDetails) {
        String username = extractUsernameFromRefreshToken(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token, getRefreshSigningKey());
    }

    // Validate Reset Token
    public boolean validateResetToken(String token) {
        try {
            extractClaims(token, getSigningKey());
            return !isTokenExpired(token, getSigningKey());
        } catch (Exception e) {
            return false;
        }
    }

    // Check if Token is Expired
    private boolean isTokenExpired(String token, SecretKey key) {
        return extractClaims(token, key).getExpiration().before(new Date());
    }

    // Extract Claims from Token
    private Claims extractClaims(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}