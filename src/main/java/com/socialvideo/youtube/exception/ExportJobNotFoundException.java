package com.socialvideo.youtube.exception;

/**
 * El {@code jobId} consultado no existe o no pertenece al usuario actual.
 *
 * <p>Se devuelve el mismo error para ambos casos (no existir vs. no ser
 * tuyo) para evitar fuga de información sobre IDs de otros usuarios.
 * HTTP 404 Not Found.</p>
 */
public class ExportJobNotFoundException extends RuntimeException {
    public ExportJobNotFoundException(String message) {
        super(message);
    }
}