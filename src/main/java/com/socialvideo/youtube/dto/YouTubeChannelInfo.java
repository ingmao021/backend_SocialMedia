package com.socialvideo.youtube.dto;

/**
 * Información mínima de un canal de YouTube.
 *
 * <p>Value object interno usado entre {@code YouTubeOAuthService} (que lo
 * obtiene al llamar a {@code channels.list?mine=true} tras el OAuth) y
 * {@code YouTubeConnectionService} (que lo persiste en {@code youtube_connections}).</p>
 *
 * <p>NO es un DTO de la API REST: nunca se serializa hacia el frontend
 * directamente. Si en el futuro hace falta exponer info del canal al
 * cliente, se usa {@link YouTubeConnectionResponse}.</p>
 *
 * @param channelId ID público del canal (ej. "UCxxx...")
 * @param title     Nombre del canal mostrado al usuario
 */
public record YouTubeChannelInfo(
        String channelId,
        String title
) {}