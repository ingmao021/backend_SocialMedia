package com.example.backend_socialmedia.shared.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilidad para generar, validar y extraer información de JWT.
 * Soporta access tokens y refresh tokens con tiempos separados.
 */
@Component
public class JwtUtils {

    @Value("${app.jwt-secret}")
    private String jwtSecret;

    @Value("${app.jwt-expiration:900000}") // 15 minutos por defecto
    private long jwtAccessExpiration;

    @Value("${app.jwt-refresh-expiration:604800000}") // 7 días por defecto
    private long jwtRefreshExpiration;

    /**
     * Genera una clave secreta segura para firmar JWT
     */
    private SecretKey getSigningKey() {
        byte[] secretKey = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(secretKey);
    }

    /**
     * Genera un par de tokens: access token y refresh token
     * @param userId ID del usuario
     * @param email Email del usuario
     * @return Map con "accessToken" y "refreshToken"
     */
    public Map<String, String> generateTokenPair(Long userId, String email) {
        Map<String, String> tokenPair = new HashMap<>();
        tokenPair.put("accessToken", generateAccessToken(userId, email));
        tokenPair.put("refreshToken", generateRefreshToken(userId, email));
        return tokenPair;
    }

    /**
     * Genera un access token JWT (corta duración)
     * @param userId ID del usuario
     * @param email Email del usuario
     * @return Token JWT firmado
     */
    public String generateAccessToken(Long userId, String email) {
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtAccessExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Genera un refresh token JWT (larga duración)
     * @param userId ID del usuario
     * @param email Email del usuario
     * @return Token JWT firmado
     */
    public String generateRefreshToken(Long userId, String email) {
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtRefreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Genera un access token a partir de un refresh token válido
     * @param refreshToken Token de refresco
     * @return Nuevo access token, o null si refresh no es válido
     */
    public String refreshAccessToken(String refreshToken) {
        try {
            Claims claims = getClaimsFromToken(refreshToken);

            // Validar que sea un refresh token
            String tokenType = claims.get("type", String.class);
            if (!"refresh".equals(tokenType)) {
                return null;
            }

            // Validar que no haya expirado
            if (isTokenExpired(refreshToken)) {
                return null;
            }

            Long userId = claims.get("userId", Long.class);
            String email = claims.getSubject();

            return generateAccessToken(userId, email);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Valida y extrae los claims de un token JWT
     * @param token Token JWT a validar
     * @return Claims del token
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrae el email (subject) del token
     * @param token Token JWT
     * @return Email del usuario
     */
    public String getEmailFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * Extrae el userId del token
     * @param token Token JWT
     * @return ID del usuario
     */
    public Long getUserIdFromToken(String token) {
        return getClaimsFromToken(token).get("userId", Long.class);
    }

    /**
     * Extrae el tipo de token (access o refresh)
     * @param token Token JWT
     * @return Tipo de token
     */
    public String getTokenType(String token) {
        return getClaimsFromToken(token).get("type", String.class);
    }

    /**
     * Verifica si el token ha expirado
     * @param token Token JWT
     * @return true si ha expirado, false si es válido
     */
    public boolean isTokenExpired(String token) {
        try {
            return getClaimsFromToken(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Valida si el token es válido
     * @param token Token JWT
     * @return true si es válido, false si no
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Getter para tiempo de expiración del access token (en ms)
     */
    public long getAccessTokenExpirationMs() {
        return jwtAccessExpiration;
    }

    /**
     * Getter para tiempo de expiración del refresh token (en ms)
     */
    public long getRefreshTokenExpirationMs() {
        return jwtRefreshExpiration;
    }
}
