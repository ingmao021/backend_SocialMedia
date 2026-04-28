package com.example.backend_socialmedia.auth.infrastructure.web;

import com.example.backend_socialmedia.auth.application.GoogleAuthUseCase;
import com.example.backend_socialmedia.shared.utils.JwtUtils;
import jakarta.servlet.http.Cookie;
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

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Manejador de autenticación OAuth2 exitosa.
 *
 * Responsabilidades:
 * 1. Procesar el usuario con Google (crear/actualizar)
 * 2. Guardar tokens de Google (access + refresh)
 * 3. Generar JWT pair (access + refresh)
 * 4. Guardar JWT en httpOnly cookies
 * 5. Redirigir al frontend con el usuario autenticado
 */
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private static final String AUTHORIZATION_HEADER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final GoogleAuthUseCase googleAuthUseCase;
    private final OAuth2AuthorizedClientService clientService;
    private final JwtUtils jwtUtils;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.cookie-secure:true}")
    private boolean cookieSecure;

    @Value("${app.cookie-same-site:Strict}")
    private String cookieSameSite;

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
            log.info("Iniciando procesamiento de autenticación OAuth2 exitosa");

            // Extraer datos de autenticación OAuth2
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
            var oAuth2Principal = token.getPrincipal();

            // Obtener Google OAuth tokens
            OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                    token.getAuthorizedClientRegistrationId(),
                    token.getName()
            );

            if (client == null || client.getAccessToken() == null) {
                log.error("OAuth2AuthorizedClient o accessToken nulo");
                redirectWithError(request, response, "OAUTH2_TOKEN_MISSING",
                        "No se pudieron obtener los tokens de Google");
                return;
            }

            // Procesar usuario con Google
            var user = googleAuthUseCase.executeWithTokens(
                    oAuth2Principal,
                    client.getAccessToken().getTokenValue(),
                    client.getRefreshToken() != null ? client.getRefreshToken().getTokenValue() : null,
                    getGoogleTokenExpiresIn(client.getAccessToken())
            );

            log.info("Usuario autenticado exitosamente: id={}, email={}", user.getId(), user.getEmail());

            // Generar JWT pair
            Map<String, String> tokenPair = jwtUtils.generateTokenPair(user.getId(), user.getEmail());
            String accessToken = tokenPair.get("accessToken");
            String refreshToken = tokenPair.get("refreshToken");

            // Guardar JWT en httpOnly cookies
            setAuthorizationCookies(response, accessToken, refreshToken);

            log.debug("JWT tokens almacenados en cookies para userId={}", user.getId());

            // Limpiar datos de sesión
            clearAuthenticationAttributes(request);

            // Redirigir al frontend
            String redirectUrl = frontendUrl + "/auth/callback?success=true";
            log.info("Redirigiendo a frontend: {}", redirectUrl);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            log.error("Error en OAuth2 success handler: {}", e.getMessage(), e);
            redirectWithError(request, response, "OAUTH2_PROCESSING_ERROR", e.getMessage());
        }
    }

    /**
     * Calcula el tiempo de expiración del access token de Google en segundos
     */
    private long getGoogleTokenExpiresIn(OAuth2AccessToken accessToken) {
        if (accessToken.getExpiresAt() == null) {
            return 3600L; // Default 1 hora
        }
        return ChronoUnit.SECONDS.between(Instant.now(), accessToken.getExpiresAt());
    }

    /**
     * Configura las cookies de autenticación (httpOnly, Secure, SameSite)
     */
    private void setAuthorizationCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        // Access Token Cookie (15 minutos)
        Cookie accessTokenCookie = createSecureCookie(
                ACCESS_TOKEN_COOKIE,
                accessToken,
                (int) (jwtUtils.getAccessTokenExpirationMs() / 1000)
        );
        response.addCookie(accessTokenCookie);

        // Refresh Token Cookie (7 días)
        Cookie refreshTokenCookie = createSecureCookie(
                REFRESH_TOKEN_COOKIE,
                refreshToken,
                (int) (jwtUtils.getRefreshTokenExpirationMs() / 1000)
        );
        response.addCookie(refreshTokenCookie);
    }

    /**
     * Crea una cookie segura (httpOnly, Secure, SameSite)
     */
    private Cookie createSecureCookie(String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);           // No accesible desde JavaScript
        cookie.setSecure(cookieSecure);     // HTTPS only en producción
        cookie.setPath("/");                // Disponible en toda la app
        cookie.setMaxAge(maxAgeSeconds);    // Tiempo de vida
        cookie.setAttribute("SameSite", cookieSameSite); // CSRF protection
        return cookie;
    }

    /**
     * Redirige al frontend con error detallado
     */
    private void redirectWithError(HttpServletRequest request, HttpServletResponse response,
                                   String errorCode, String errorMessage) throws IOException {
        log.warn("OAuth2 error: code={}, message={}", errorCode, errorMessage);
        String redirectUrl = frontendUrl + "/auth/callback?error=" + errorCode + "&reason=" +
                            java.net.URLEncoder.encode(errorMessage, java.nio.charset.StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
