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
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
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
                // CSRF: habilitar con CookieCsrfTokenRepository en entornos stateful (SPA)
                // Ignorar endpoints de OAuth2 y autenticación que gestionan redirecciones
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                "/api/auth/**",
                                "/oauth2/**",
                                "/login/**"
                        )
                )
                // Configuración de cookies de sesión
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers(
                                "/",
                                "/health",
                                "/api/auth/status",
                                "/api/auth/google-url",
                                "/api/auth/debug/token",  // Solo desarrollo
                                "/login/**",
                                "/oauth2/**",
                                "/error"
                        ).permitAll()
                        // Todos los demás endpoints requieren autenticación
                        .anyRequest().authenticated()
                )
                // Cuando una API sin auth intenta acceder, devolver 401 en vez de redirigir
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // Configuración OAuth2 Login
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(authorizationRequestResolver)
                        )
                )
                // JWT authentication filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}