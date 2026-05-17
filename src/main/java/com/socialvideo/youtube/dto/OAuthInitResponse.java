package com.socialvideo.youtube.dto;

/**
 * Respuesta de {@code POST /api/youtube/oauth/connect}.
 *
 * <p>El frontend recibe la URL de Google a la que debe redirigir al usuario
 * para iniciar el consentimiento OAuth. El {@code state} también se incluye
 * para que el frontend pueda guardarlo si quisiera correlacionar, aunque la
 * validación anti-CSRF la realiza exclusivamente el backend en el callback.</p>
 *
 * @param authorizationUrl URL completa de Google (accounts.google.com/o/oauth2/v2/auth?...)
 * @param state            String criptográfico anti-CSRF asociado al usuario
 */
public record OAuthInitResponse(
        String authorizationUrl,
        String state
) {}