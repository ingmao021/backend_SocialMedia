package com.example.backend_socialmedia.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Configuración CORS para el backend.
 *
 * NOTA IMPORTANTE:
 * - NO aplicar CORS a Google OAuth (Google maneja esto)
 * - Solo aplicar a endpoints /api/** del backend
 * - Las cookies httpOnly se configuran automáticamente
 * - Credentials: true permite envío de cookies en requests CORS
 */
@Configuration
public class CorsConfig {

    @Value("${app.frontend-url}")
    private String frontendUrl;
    @Value("${app.cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Build allowed origin patterns. Allow either a configured list (app.cors.allowed-origins)
        // or fall back to sensible defaults (frontendUrl, localhost, vercel)
        List<String> allowedPatterns;
        if (corsAllowedOrigins != null && !corsAllowedOrigins.isBlank()) {
            allowedPatterns = Arrays.stream(corsAllowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } else {
            allowedPatterns = List.of(
                    frontendUrl,
                    "http://localhost:*",
                    "https://*.vercel.app"
            );
        }

        config.setAllowedOriginPatterns(allowedPatterns);

        // Métodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Headers permitidos en requests (permitir todos para evitar problemas con encabezados personalizados)
        config.setAllowedHeaders(List.of("*"));

        // Headers expuestos en responses
        config.setExposedHeaders(List.of("Authorization", "X-Total-Count"));

        // Permitir credentials (cookies httpOnly)
        config.setAllowCredentials(true);

        // TTL del preflight cache (segundos)
        config.setMaxAge(3600L);

        // Registro de rutas
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // CORS solo para API endpoints
        source.registerCorsConfiguration("/api/**", config);

        return source;
    }
}


