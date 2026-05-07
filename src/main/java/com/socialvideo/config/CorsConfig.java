package com.socialvideo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final AppProperties appProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Origins configurados según tu solicitud (desarrollo y producción)
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173", 
            "https://backend-socialmedia-ixsm.onrender.com"
        ));
        
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true); // Permite credenciales (necesario para algunos flujos)
        config.setMaxAge(3600L); // Max-Age 3600 segundos (1 hora) para cachear preflight requests

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Se aplica a TODOS los endpoints
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
