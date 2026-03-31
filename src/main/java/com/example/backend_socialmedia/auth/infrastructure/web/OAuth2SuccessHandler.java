package com.example.backend_socialmedia.auth.infrastructure.web;

import com.example.backend_socialmedia.auth.application.GoogleAuthUseCase;
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

@Component
public class OAuth2SuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final GoogleAuthUseCase googleAuthUseCase;
    private final OAuth2AuthorizedClientService clientService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public OAuth2SuccessHandler(GoogleAuthUseCase googleAuthUseCase,
                                OAuth2AuthorizedClientService clientService) {
        this.googleAuthUseCase = googleAuthUseCase;
        this.clientService     = clientService;
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
        googleAuthUseCase.executeWithTokens(
                token.getPrincipal(),
                accessToken.getTokenValue(),
                refreshToken != null ? refreshToken.getTokenValue() : null,
                expiresIn
        );

        // Redirige al frontend después del login exitoso
        getRedirectStrategy().sendRedirect(
                request, response, frontendUrl + "/chat"
        );
    }
}