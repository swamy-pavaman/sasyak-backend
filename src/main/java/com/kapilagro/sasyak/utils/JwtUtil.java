package com.kapilagro.sasyak.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.PrivateJwk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {



    @Value("${jwt.secret.key}")
    private  String SECRET_kEY;

    @Value("${jwt.refresh.key}")
    private String REFRESH_SECRET_KEY;


//
//    // TODO replace this hardcoded secret keys to application properties file
//    private static final String SECRET_kEY ="KA92Ab8OphARt/lQwY6u5Zn+LkwISP6m9ABjI3JQfVo=";
//    private static final String REFRESH_SECRET_KEY ="HyaFfpChC8IekjRGc5loPYid4/uHekm0dBmlJaYnvq0=";


    private static final long ACCESS_TOKEN_EXPIRATION = (1000 * 60 * 30)*6; // 90 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 24 * 20; // 7 days

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_kEY));
    }

    private SecretKey getRefreshSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(REFRESH_SECRET_KEY));
    }

    // Generate Access Token
    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(userDetails, getSigningKey(), ACCESS_TOKEN_EXPIRATION);
    }

    // Generate Refresh Token
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // Add a unique token ID to prevent reuse
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

//    public static String generateAccessTokenSecret() {
//        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
//        return Encoders.BASE64.encode(key.getEncoded());
//    }

    /**
     * Generates a Base64-encoded secret key for the refresh token.
     * You can choose to use the same algorithm (HS256) or a different one,
     * as long as the key length meets the minimum requirement.
     */
//    public static String generateRefreshTokenSecret() {
//        // Here we are using the same HS256 algorithm, but you could opt for HS512 if desired.
//        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
//        return Encoders.BASE64.encode(key.getEncoded());
//    }
}