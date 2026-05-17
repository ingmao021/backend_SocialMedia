package com.socialvideo.youtube.exception;

/**
 * El upload a YouTube falló por un error del lado de YouTube
 * (5xx, cuota agotada, error de red, etc.).
 *
 * <p>HTTP 502 Bad Gateway. El usuario puede reintentar manualmente.</p>
 */
public class YouTubeUploadFailedException extends RuntimeException {
    public YouTubeUploadFailedException(String message) {
        super(message);
    }
}