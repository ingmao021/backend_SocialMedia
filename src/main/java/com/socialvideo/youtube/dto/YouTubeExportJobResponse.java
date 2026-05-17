package com.socialvideo.youtube.dto;

import com.socialvideo.youtube.entity.YouTubeExportStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Respuesta del estado de un job de exportación.
 *
 * <p>Devuelta por {@code POST /api/videos/{id}/youtube/export} (con
 * HTTP 202 Accepted al crear el job) y por {@code GET /api/youtube/exports/{jobId}}
 * (durante el polling). El frontend hace polling cada 2 segundos hasta que
 * {@code status} sea {@code COMPLETED} o {@code FAILED}.</p>
 *
 * <p>{@code bytesUploaded} y {@code bytesTotal} permiten al frontend dibujar
 * una barra de progreso real. {@code progressPercent} viene precalculado
 * para que el frontend no tenga que manejar el caso {@code bytesTotal == null}.</p>
 *
 * @param jobId            ID del job (UUID), también la PK en BD
 * @param videoId          ID del video que se exporta
 * @param status           Estado actual: PENDING / UPLOADING / COMPLETED / FAILED
 * @param bytesUploaded    Bytes subidos a YouTube hasta ahora
 * @param bytesTotal       Tamaño total del archivo (null hasta que el worker arranca)
 * @param progressPercent  Porcentaje 0–100, null si {@code bytesTotal} aún se desconoce
 * @param youtubeVideoId   ID público del video en YouTube (solo cuando COMPLETED)
 * @param youtubeVideoUrl  URL pública (https://youtube.com/watch?v=...)
 * @param errorCode        Código semántico del error (solo cuando FAILED)
 * @param errorMessage     Mensaje de error legible (solo cuando FAILED)
 * @param createdAt        Cuándo se creó el job
 * @param startedAt        Cuándo el worker empezó (null mientras PENDING)
 * @param completedAt      Cuándo terminó (null mientras PENDING o UPLOADING)
 */
public record YouTubeExportJobResponse(
        UUID jobId,
        UUID videoId,
        YouTubeExportStatus status,
        long bytesUploaded,
        Long bytesTotal,
        Double progressPercent,
        String youtubeVideoId,
        String youtubeVideoUrl,
        String errorCode,
        String errorMessage,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt
) {}