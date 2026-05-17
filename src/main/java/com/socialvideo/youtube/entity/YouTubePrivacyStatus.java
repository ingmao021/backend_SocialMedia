package com.socialvideo.youtube.entity;

/**
 * Estados de privacidad soportados por la YouTube Data API v3 al subir un video.
 *
 * <p>Los nombres del enum corresponden 1:1 con la columna {@code privacy_status}
 * de la tabla {@code youtube_export_jobs}. Para enviarlos a la API de YouTube
 * deben pasarse en minúsculas (la API espera {@code "private"}, {@code "unlisted"},
 * {@code "public"}), conversión que hace el cliente HTTP, no esta capa.</p>
 *
 * <p><b>Importante:</b> hasta que la app de Google Cloud pase la auditoría de
 * YouTube API Services, los uploads con {@link #PUBLIC} o {@link #UNLISTED}
 * serán forzados a {@link #PRIVATE} por YouTube de forma silenciosa.</p>
 */
public enum YouTubePrivacyStatus {
    PRIVATE,
    UNLISTED,
    PUBLIC
}