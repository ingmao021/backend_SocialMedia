package com.example.backend_socialmedia.shared.exception;

public class OAuthTokenNotFoundException extends RuntimeException {
    public OAuthTokenNotFoundException(String userId) {
        super("No se encontró token OAuth para userId: " + userId);
    }
}