package com.socialvideo.youtube.entity;

/**
 * Ciclo de vida de un job de exportación a YouTube.
 *
 * <ul>
 *   <li>{@link #PENDING}    — Job recién creado, esperando a que el worker lo recoja.</li>
 *   <li>{@link #UPLOADING}  — El worker ya inició la subida resumable a YouTube.</li>
 *   <li>{@link #COMPLETED}  — YouTube confirmó la recepción. {@code youtube_video_id} está poblado.</li>
 *   <li>{@link #FAILED}     — Error definitivo. {@code error_code} y {@code error_message} indican la causa.</li>
 * </ul>
 *
 * <p>Los valores se persisten como VARCHAR(20) en la columna {@code status}
 * con CHECK constraint. El nombre del enum DEBE coincidir exactamente con
 * los strings declarados en la migración V4.</p>
 */
public enum YouTubeExportStatus {
    PENDING,
    UPLOADING,
    COMPLETED,
    FAILED
}