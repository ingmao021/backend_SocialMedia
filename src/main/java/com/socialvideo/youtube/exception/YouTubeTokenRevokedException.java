package com.socialvideo.youtube.exception;

/**
 * El refresh token de la conexión OAuth ya no es válido.
 *
 * <p>Puede ocurrir si el usuario revocó manualmente el acceso desde
 * {@code myaccount.google.com/permissions}, si la cuenta de Google
 * se eliminó, o si Google invalidó el token por inactividad.</p>
 *
 * <p>HTTP 401 Unauthorized. El frontend debe pedir al usuario
 * que vuelva a conectar su cuenta.</p>
 */
public class YouTubeTokenRevokedException extends RuntimeException {
    public YouTubeTokenRevokedException(String message) {
        super(message);
    }
}