package com.socialvideo.youtube.exception;

/**
 * Error interno al cifrar o descifrar el refresh token.
 *
 * <p>Indica un problema de configuración (clave AES inválida, IV corrupto)
 * o un fallo del proveedor criptográfico. HTTP 500 Internal Server Error.
 * El mensaje expuesto al cliente NO revela detalles de cifrado.</p>
 */
public class EncryptionException extends RuntimeException {
    public EncryptionException(String message) {
        super(message);
    }

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}