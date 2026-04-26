package com.example.backend_socialmedia.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path
) {
    public static ApiErrorResponse of(int status, String error, String code, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, code, message, path);
    }
}
