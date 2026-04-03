package com.example.backend_socialmedia.auth.infrastructure.web;

import com.example.backend_socialmedia.auth.application.GoogleAuthUseCase;
import com.example.backend_socialmedia.shared.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Component
public class OAuth2SuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final GoogleAuthUseCase googleAuthUseCase;
    private final OAuth2AuthorizedClientService clientService;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public OAuth2SuccessHandler(GoogleAuthUseCase googleAuthUseCase,
                                OAuth2AuthorizedClientService clientService,
                                JwtUtils jwtUtils,
                                ObjectMapper objectMapper) {
        this.googleAuthUseCase = googleAuthUseCase;
        this.clientService     = clientService;
        this.jwtUtils          = jwtUtils;
        this.objectMapper      = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException {

        OAuth2AuthenticationToken token =
                (OAuth2AuthenticationToken) authentication;

        // Obtiene el cliente autorizado (contiene accessToken y refreshToken)
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                token.getAuthorizedClientRegistrationId(),
                token.getName()
        );

        OAuth2AccessToken  accessToken  = client.getAccessToken();
        OAuth2RefreshToken refreshToken = client.getRefreshToken();

        long expiresIn = (accessToken.getExpiresAt() != null)
                ? ChronoUnit.SECONDS.between(
                java.time.Instant.now(),
                accessToken.getExpiresAt())
                : 3600L;

        // Llama al caso de uso con los tokens
        var user = googleAuthUseCase.executeWithTokens(
                token.getPrincipal(),
                accessToken.getTokenValue(),
                refreshToken != null ? refreshToken.getTokenValue() : null,
                expiresIn
        );

        // Genera JWT
        String jwt = jwtUtils.generateToken(user.getId(), user.getEmail());

        // Responde con JSON en lugar de redirigir
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("token", jwt);
        responseBody.put("user", Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "picture", user.getPicture()
        ));

        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }
}