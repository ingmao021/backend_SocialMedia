package com.example.backend_socialmedia.auth.infrastructure.web;

import com.example.backend_socialmedia.auth.application.GoogleAuthUseCase;
import com.example.backend_socialmedia.shared.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final GoogleAuthUseCase googleAuthUseCase;
    private final OAuth2AuthorizedClientService clientService;
    private final JwtUtils jwtUtils;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public OAuth2SuccessHandler(GoogleAuthUseCase googleAuthUseCase,
                                OAuth2AuthorizedClientService clientService,
                                JwtUtils jwtUtils) {
        this.googleAuthUseCase = googleAuthUseCase;
        this.clientService = clientService;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            log.info("OAuth2 autenticación exitosa, procesando...");

            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;

            OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                    token.getAuthorizedClientRegistrationId(),
                    token.getName()
            );

            if (client == null || client.getAccessToken() == null) {
                log.error("OAuth2AuthorizedClient o accessToken nulo");
                getRedirectStrategy().sendRedirect(request, response,
                        frontendUrl + "/auth/error?reason=token_missing");
                return;
            }

            OAuth2AccessToken accessToken = client.getAccessToken();
            OAuth2RefreshToken refreshToken = client.getRefreshToken();

            long expiresIn = accessToken.getExpiresAt() != null
                    ? ChronoUnit.SECONDS.between(Instant.now(), accessToken.getExpiresAt())
                    : 3600L;

            var user = googleAuthUseCase.executeWithTokens(
                    token.getPrincipal(),
                    accessToken.getTokenValue(),
                    refreshToken != null ? refreshToken.getTokenValue() : null,
                    expiresIn
            );

            log.info("Usuario autenticado: id={}, email={}", user.getId(), user.getEmail());

            String jwt = jwtUtils.generateToken(user.getId(), user.getEmail());

            // Redirigir al frontend con el token en la URL para que el SPA lo capture
            String redirectUrl = UriComponentsBuilder
                    .fromUriString(frontendUrl + "/auth/callback")
                    .queryParam("token", jwt)
                    .build()
                    .toUriString();

            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            log.error("Error en OAuth2 success handler: {}", e.getMessage(), e);
            getRedirectStrategy().sendRedirect(request, response,
                    frontendUrl + "/auth/error?reason=server_error");
        }
    }
}
