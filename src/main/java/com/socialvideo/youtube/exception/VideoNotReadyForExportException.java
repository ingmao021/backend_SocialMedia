package com.socialvideo.youtube.exception;

/**
 * Se intenta exportar a YouTube un video cuyo estado no es {@code COMPLETED}.
 *
 * <p>Por ejemplo, si el video aún está {@code PROCESSING} en Vertex AI,
 * o si falló su generación ({@code FAILED}). HTTP 422 Unprocessable Entity.</p>
 */
public class VideoNotReadyForExportException extends RuntimeException {
    public VideoNotReadyForExportException(String message) {
        super(message);
    }
}