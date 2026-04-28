package com.example.backend_socialmedia.auth.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador de fallos en autenticación OAuth2.
 *
 * Responsabilidades:
 * - Capturar excepciones durante OAuth2
 * - Mapear errores específicos de Google
 * - Redirigir con mensaje de error
 * - Logging estructurado
 */
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2FailureHandler.class);

    private final ObjectMapper objectMapper;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public OAuth2FailureHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        log.error("OAuth2 autenticación fallida", exception);

        // Mapear error específico de OAuth2
        ErrorDetails errorDetails = mapOAuth2Error(exception);

        log.warn("Error OAuth2: code={}, message={}, details={}",
                errorDetails.code, errorDetails.message, errorDetails.details);

        // Redirigir al frontend con detalles del error
        String errorMessage = URLEncoder.encode(errorDetails.message, StandardCharsets.UTF_8);
        String errorDetails_encoded = URLEncoder.encode(errorDetails.details, StandardCharsets.UTF_8);
        String redirectUrl = String.format("%s/auth/callback?error=%s&message=%s&details=%s",
                frontendUrl,
                errorDetails.code,
                errorMessage,
                errorDetails_encoded);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /**
     * Mapea excepciones de OAuth2 a mensajes de error legibles
     */
    private ErrorDetails mapOAuth2Error(AuthenticationException exception) {
        String code = "AUTHENTICATION_FAILED";
        String message = "Error de autenticación";
        String details = exception.getMessage();

        if (exception instanceof OAuth2AuthenticationException oauthException) {
            String errorCode = oauthException.getError().getErrorCode();
            String errorDescription = oauthException.getError().getDescription();

            return switch (errorCode) {
                case "invalid_grant" -> new ErrorDetails(
                        "INVALID_CREDENTIALS",
                        "Las credenciales de Google son inválidas",
                        "Debes volver a iniciar sesión en Google. Revoca el acceso en myaccount.google.com/permissions"
                );
                case "access_denied" -> new ErrorDetails(
                        "ACCESS_DENIED",
                        "Acceso denegado",
                        "Debes autorizar el acceso a tu cuenta de Google"
                );
                case "invalid_client" -> new ErrorDetails(
                        "INVALID_CLIENT",
                        "Configuración inválida del cliente OAuth",
                        "El GOOGLE_CLIENT_ID o GOOGLE_CLIENT_SECRET son incorrectos"
                );
                case "unauthorized_client" -> new ErrorDetails(
                        "UNAUTHORIZED_CLIENT",
                        "Cliente no autorizado",
                        "Verifica que la configuración de OAuth coincida con Google Cloud Console"
                );
                case "invalid_scope" -> new ErrorDetails(
                        "INVALID_SCOPE",
                        "Permisos inválidos",
                        "Los scopes solicitados no son válidos"
                );
                case "server_error" -> new ErrorDetails(
                        "SERVER_ERROR",
                        "Error en el servidor de Google",
                        "Los servidores de Google están experimentando problemas. Intenta más tarde"
                );
                case "temporarily_unavailable" -> new ErrorDetails(
                        "SERVICE_UNAVAILABLE",
                        "Servicio no disponible",
                        "Google está temporalmente inaccesible. Intenta más tarde"
                );
                default -> new ErrorDetails(
                        "OAUTH2_ERROR",
                        "Error de autenticación con Google",
                        errorDescription != null ? errorDescription : errorCode
                );
            };
        }

        // Error genérico no-OAuth2
        return new ErrorDetails(code, message, details);
    }

    /**
     * Clase para encapsular detalles de error
     */
    private static class ErrorDetails {
        String code;
        String message;
        String details;

        ErrorDetails(String code, String message, String details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }
    }
}

