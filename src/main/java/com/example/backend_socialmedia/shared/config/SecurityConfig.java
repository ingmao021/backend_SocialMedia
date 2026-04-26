package com.example.backend_socialmedia.shared.config;

import com.example.backend_socialmedia.auth.application.GoogleAuthUseCase;
import com.example.backend_socialmedia.auth.infrastructure.web.CustomAuthorizationRequestResolver;
import com.example.backend_socialmedia.auth.infrastructure.web.OAuth2SuccessHandler;
import com.example.backend_socialmedia.auth.infrastructure.web.OAuth2FailureHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final GoogleAuthUseCase googleAuthUseCase;
    private final CorsConfigurationSource corsConfigurationSource;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final CustomAuthorizationRequestResolver authorizationRequestResolver;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public SecurityConfig(GoogleAuthUseCase googleAuthUseCase,
                          CorsConfigurationSource corsConfigurationSource,
                          OAuth2SuccessHandler oAuth2SuccessHandler,
                          OAuth2FailureHandler oAuth2FailureHandler,
                          CustomAuthorizationRequestResolver authorizationRequestResolver,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.googleAuthUseCase = googleAuthUseCase;
        this.corsConfigurationSource = corsConfigurationSource;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.oAuth2FailureHandler = oAuth2FailureHandler;
        this.authorizationRequestResolver = authorizationRequestResolver;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/health",
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
                        // Manejador de éxito en autenticación
                        .successHandler(oAuth2SuccessHandler)
                        // Manejador de fallos en autenticación
                        .failureHandler(oAuth2FailureHandler)
                        // Personalizar parámetros de autorización (access_type=offline, prompt=consent)
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(authorizationRequestResolver)
                        )
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}