package com.example.backend_socialmedia.shared.exception;

import com.example.backend_socialmedia.shared.web.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validación fallida en {}: {}", req.getRequestURI(), message);
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, "Bad Request", "VALIDATION_ERROR", message, req.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("Argumento inválido en {}: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, "Bad Request", "INVALID_ARGUMENT", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(VideoNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleVideoNotFound(
            VideoNotFoundException ex, HttpServletRequest req) {
        log.warn("Video no encontrado en {}: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(404, "Not Found", "VIDEO_NOT_FOUND", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(VideoGenerationException.class)
    public ResponseEntity<ApiErrorResponse> handleVideoGeneration(
            VideoGenerationException ex, HttpServletRequest req) {
        log.error("Error en generación de video en {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(500, "Internal Server Error", "VIDEO_GENERATION_ERROR", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(YouTubePublishException.class)
    public ResponseEntity<ApiErrorResponse> handleYouTubePublish(
            YouTubePublishException ex, HttpServletRequest req) {
        log.error("Error al publicar en YouTube en {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiErrorResponse.of(502, "Bad Gateway", "YOUTUBE_PUBLISH_ERROR", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(InvalidGoogleTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidGoogleToken(
            InvalidGoogleTokenException ex, HttpServletRequest req) {
        log.warn("Token Google inválido en {}: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(401, "Unauthorized", "TOKEN_INVALIDO", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(OAuthTokenNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOAuthTokenNotFound(
            OAuthTokenNotFoundException ex, HttpServletRequest req) {
        log.warn("Token OAuth no encontrado en {}: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(404, "Not Found", "TOKEN_NO_ENCONTRADO", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(401, "Unauthorized", "NO_AUTENTICADO", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(403, "Forbidden", "ACCESO_DENEGADO", "Sin permisos suficientes para este recurso", req.getRequestURI()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntime(RuntimeException ex, HttpServletRequest req) {
        log.error("Error inesperado en {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(500, "Internal Server Error", "ERROR_INTERNO", "Ocurrió un error inesperado", req.getRequestURI()));
    }
}
