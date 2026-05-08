package com.socialvideo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fields = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid",
                        (a, b) -> a));
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_ERROR", "Datos inválidos", fields));
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ApiError> handleQuota(QuotaExceededException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("QUOTA_EXCEEDED", e.getMessage(), null));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleAuth(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("INVALID_CREDENTIALS", e.getMessage(), null));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleEmailConflict(EmailAlreadyRegisteredException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("EMAIL_ALREADY_REGISTERED", e.getMessage(), null));
    }

    @ExceptionHandler(InvalidGoogleTokenException.class)
    public ResponseEntity<ApiError> handleInvalidGoogleToken(InvalidGoogleTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("INVALID_GOOGLE_TOKEN", e.getMessage(), null));
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<ApiError> handleInvalidFileType(InvalidFileTypeException e) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_FILE_TYPE", e.getMessage(), null));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("NOT_FOUND", e.getMessage(), null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(new ApiError("BAD_REQUEST", e.getMessage(), null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUpload(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiError("PAYLOAD_TOO_LARGE", "El archivo excede el tamaño máximo de 2MB", null));
    }

    @ExceptionHandler(VideoGenerationException.class)
    public ResponseEntity<ApiError> handleVideoGeneration(VideoGenerationException e) {
        log.error("Error en generación de video", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("VIDEO_GENERATION_ERROR", e.getMessage(), null));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiError> handleGeneric(Throwable e) {
        log.error("Error no manejado", e);
        // Devolver el mensaje real de la excepción para facilitar el debug en Render
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_ERROR", "Error interno: " + e.getMessage() + " | Clase: " + e.getClass().getName(), null));
    }
}
