package com.socialvideo.youtube.exception;

import com.socialvideo.exception.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handlers de excepciones específicas del módulo YouTube.
 *
 * <p>Limita su scope a los controllers del paquete
 * {@code com.socialvideo.youtube} usando {@code basePackages}. Las
 * excepciones de otros módulos siguen siendo procesadas por el
 * {@code GlobalExceptionHandler} del proyecto, que permanece intocado.</p>
 *
 * <p>Convención de códigos de error (campo {@code code} de {@link ApiError}):
 * SCREAMING_SNAKE_CASE con prefijo {@code YOUTUBE_*} para distinguirlos
 * claramente de los códigos globales.</p>
 */
@RestControllerAdvice(basePackages = "com.socialvideo.youtube")
@Slf4j
public class YouTubeExceptionHandler {

    @ExceptionHandler(YouTubeNotConnectedException.class)
    public ResponseEntity<ApiError> handleNotConnected(YouTubeNotConnectedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("YOUTUBE_NOT_CONNECTED", e.getMessage(), null));
    }

    @ExceptionHandler(YouTubeTokenRevokedException.class)
    public ResponseEntity<ApiError> handleTokenRevoked(YouTubeTokenRevokedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("YOUTUBE_TOKEN_REVOKED", e.getMessage(), null));
    }

    @ExceptionHandler(YouTubeUploadFailedException.class)
    public ResponseEntity<ApiError> handleUploadFailed(YouTubeUploadFailedException e) {
        log.error("Fallo al subir video a YouTube", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError("YOUTUBE_UPLOAD_FAILED", e.getMessage(), null));
    }

    @ExceptionHandler(InvalidOAuthStateException.class)
    public ResponseEntity<ApiError> handleInvalidState(InvalidOAuthStateException e) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_OAUTH_STATE", e.getMessage(), null));
    }

    @ExceptionHandler(VideoNotReadyForExportException.class)
    public ResponseEntity<ApiError> handleVideoNotReady(VideoNotReadyForExportException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError("VIDEO_NOT_READY_FOR_EXPORT", e.getMessage(), null));
    }

    @ExceptionHandler(ExportJobNotFoundException.class)
    public ResponseEntity<ApiError> handleJobNotFound(ExportJobNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("EXPORT_JOB_NOT_FOUND", e.getMessage(), null));
    }

    @ExceptionHandler(EncryptionException.class)
    public ResponseEntity<ApiError> handleEncryption(EncryptionException e) {
        log.error("Error de cifrado en módulo YouTube", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_ERROR", "Error interno procesando la solicitud", null));
    }
}