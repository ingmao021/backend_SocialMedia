package com.example.backend_socialmedia.shared.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Token de Google inválido → 401
    @ExceptionHandler(InvalidGoogleTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidGoogleToken(
            InvalidGoogleTokenException ex) {
        log.warn("Token de Google inválido: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(errorBody("TOKEN_INVALIDO", ex.getMessage()));
    }

    // Token OAuth no encontrado en BD → 404
    @ExceptionHandler(OAuthTokenNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTokenNotFound(
            OAuthTokenNotFoundException ex) {
        log.warn("Token OAuth no encontrado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorBody("TOKEN_NO_ENCONTRADO", ex.getMessage()));
    }

    // Cualquier otro RuntimeException → 500
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(
            RuntimeException ex) {

        // Si es usuario no encontrado, devolver 404
        if (ex.getMessage() != null && (ex.getMessage().contains("Usuario no encontrado") ||
            ex.getMessage().contains("No encontrado"))) {
            log.warn("Usuario no encontrado: {}", ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(errorBody("USUARIO_NO_ENCONTRADO", ex.getMessage()));
        }

        // Otros errores → 500
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("ERROR_INTERNO", "Ocurrió un error inesperado: " + ex.getMessage()));
    }

    // Método auxiliar para construir respuesta de error uniforme
    private Map<String, Object> errorBody(String code, String message) {
        return Map.of(
                "code",      code,
                "message",   message,
                "timestamp", Instant.now().toString()
        );
    }
}