package com.example.backend_socialmedia.auth.infrastructure.web;

import com.example.backend_socialmedia.auth.application.GoogleAuthUseCase;
import com.example.backend_socialmedia.shared.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

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

        try {
            log.info("OAuth2 autenticación iniciada");

            OAuth2AuthenticationToken token =
                    (OAuth2AuthenticationToken) authentication;

            // Obtiene el cliente autorizado (contiene accessToken y refreshToken)
            OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                    token.getAuthorizedClientRegistrationId(),
                    token.getName()
            );

            if (client == null) {
                log.error("No se pudo cargar el cliente autorizado de OAuth2");
                throw new IOException("No se pudo cargar el cliente autorizado de OAuth2");
            }

            OAuth2AccessToken  accessToken  = client.getAccessToken();
            OAuth2RefreshToken refreshToken = client.getRefreshToken();

            if (accessToken == null) {
                log.error("Access token es null");
                throw new IOException("Access token no disponible");
            }

            long expiresIn = (accessToken.getExpiresAt() != null)
                    ? ChronoUnit.SECONDS.between(
                    java.time.Instant.now(),
                    accessToken.getExpiresAt())
                    : 3600L;

            log.debug("Access token obtenido, expira en {} segundos", expiresIn);

            // Llama al caso de uso con los tokens
            var user = googleAuthUseCase.executeWithTokens(
                    token.getPrincipal(),
                    accessToken.getTokenValue(),
                    refreshToken != null ? refreshToken.getTokenValue() : null,
                    expiresIn
            );

            log.info("Usuario autenticado: email={}, id={}", user.getEmail(), user.getId());

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
            responseBody.put("success", true);
            responseBody.put("message", "Autenticación exitosa");

            response.getWriter().write(objectMapper.writeValueAsString(responseBody));

        } catch (Exception e) {
            log.error("Error en autenticación OAuth2: {}", e.getMessage(), e);

            // En caso de error, devolver JSON con error
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("error", "AUTHENTICATION_ERROR");
            errorBody.put("message", "Error durante la autenticación: " + e.getMessage());
            errorBody.put("timestamp", System.currentTimeMillis());

            response.getWriter().write(objectMapper.writeValueAsString(errorBody));
        }
    }
}