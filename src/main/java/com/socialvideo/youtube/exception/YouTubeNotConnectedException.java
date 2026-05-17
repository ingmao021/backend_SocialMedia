package com.socialvideo.youtube.exception;

/**
 * El usuario intenta exportar un video a YouTube pero no tiene
 * una conexión OAuth activa con su cuenta.
 *
 * <p>HTTP 409 Conflict.</p>
 */
public class YouTubeNotConnectedException extends RuntimeException {
    public YouTubeNotConnectedException(String message) {
        super(message);
    }
}