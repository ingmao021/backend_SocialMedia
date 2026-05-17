package com.socialvideo.youtube.dto;

import java.time.Instant;

/**
 * Respuesta de {@code GET /api/youtube/connection}.
 *
 * <p>Devuelve únicamente información segura para mostrar al usuario en el
 * frontend (nombre del canal, fecha de conexión). NO expone tokens, ni el
 * {@code googleSub}, ni ningún dato cifrado o sensible.</p>
 *
 * @param channelId    ID público del canal de YouTube (ej. "UCxxx...")
 * @param channelTitle Nombre del canal mostrado al usuario
 * @param connectedAt  Cuándo el usuario conectó esta cuenta
 * @param active       True si la conexión sigue activa (no revocada)
 */
public record YouTubeConnectionResponse(
        String channelId,
        String channelTitle,
        Instant connectedAt,
        boolean active
) {}