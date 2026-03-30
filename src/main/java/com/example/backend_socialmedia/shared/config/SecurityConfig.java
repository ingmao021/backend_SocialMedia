package com.example.backend_socialmedia.shared.config;

import com.example.backend_socialmedia.auth.application.GoogleAuthUseCase;
import com.example.backend_socialmedia.auth.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;
import java.io.IOException;


@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final GoogleAuthUseCase googleAuthUseCase;
    private final CorsConfigurationSource corsConfigurationSource;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public SecurityConfig(GoogleAuthUseCase googleAuthUseCase,
                          CorsConfigurationSource corsConfigurationSource) {
        this.googleAuthUseCase = googleAuthUseCase;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/status",
                                "/api/auth/google-url",
                                "/login/**",
                                "/oauth2/**",
                                "/error",
                                "/hola",
                                "/api/check",
                                "/api/status"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(this::handleOAuth2Success)
                );

        return http.build();
    }

    // El Success Handler ahora vive aquí — guarda el usuario y redirige al frontend
    private void handleOAuth2Success(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        // Guarda o actualiza el usuario en la base de datos
        User user = googleAuthUseCase.execute(oAuth2User);
        // Redirige al frontend
        response.sendRedirect(frontendUrl + "/chat");
    }
}
