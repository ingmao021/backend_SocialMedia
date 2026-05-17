package com.socialvideo.youtube.exception;

/**
 * El parámetro {@code state} recibido en el callback OAuth no es válido:
 * no existe en BD, ya fue consumido, o expiró.
 *
 * <p>Protección anti-CSRF según RFC 6749. HTTP 400 Bad Request.</p>
 */
public class InvalidOAuthStateException extends RuntimeException {
    public InvalidOAuthStateException(String message) {
        super(message);
    }
}