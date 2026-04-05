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
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador de fallos en autenticación OAuth2
 * Devuelve errores en formato JSON con mensajes claros
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

        log.error("OAuth2 autenticación fallida: {}", exception.getMessage(), exception);

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String errorCode = "AUTHENTICATION_FAILED";
        String errorMessage = "Credenciales inválidas o expiradas";
        String errorDetails = exception.getMessage();

        // Identificar el tipo específico de error
        if (exception instanceof OAuth2AuthenticationException oauthException) {
            String errorParameter = oauthException.getError().getErrorCode();

            switch (errorParameter) {
                case "invalid_grant" -> {
                    errorCode = "INVALID_CREDENTIALS";
                    errorMessage = "Las credenciales de Google son inválidas o han expirado";
                    errorDetails = "Debes iniciar sesión nuevamente en Google";
                }
                case "access_denied" -> {
                    errorCode = "ACCESS_DENIED";
                    errorMessage = "Acceso denegado. Verifica los permisos en tu cuenta de Google";
                    errorDetails = "Revisa los permisos solicitados y vuelve a intentar";
                }
                case "invalid_client" -> {
                    errorCode = "INVALID_CLIENT";
                    errorMessage = "Configuración inválida del cliente OAuth2";
                    errorDetails = "Verifica que GOOGLE_CLIENT_ID y GOOGLE_CLIENT_SECRET sean correctos";
                }
                case "unauthorized_client" -> {
                    errorCode = "UNAUTHORIZED_CLIENT";
                    errorMessage = "El cliente no está autorizado";
                    errorDetails = "Verifica la configuración de OAuth en Google Cloud Console";
                }
                case "server_error" -> {
                    errorCode = "SERVER_ERROR";
                    errorMessage = "Error en el servidor de autenticación";
                    errorDetails = "Intenta nuevamente más tarde";
                }
            }
        }

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("error", errorCode);
        errorBody.put("message", errorMessage);
        errorBody.put("details", errorDetails);
        errorBody.put("timestamp", System.currentTimeMillis());
        errorBody.put("path", request.getRequestURI());

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}

