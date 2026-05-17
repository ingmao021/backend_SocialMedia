package com.socialvideo.youtube.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Job de exportación de un video a YouTube.
 *
 * <p>Una fila por solicitud de exportación. El {@code id} (UUID) es el
 * {@code jobId} que el frontend usa para consultar el estado por polling.
 * Los campos {@code bytesUploaded} / {@code bytesTotal} permiten dibujar
 * una barra de progreso real durante la subida.</p>
 */
@Entity
@Table(name = "youtube_export_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YouTubeExportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /** FK a {@code users.id}. Propietario del job. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** FK a {@code videos.id}. El video a subir. */
    @Column(name = "video_id", columnDefinition = "uuid", nullable = false)
    private UUID videoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private YouTubeExportStatus status;

    /** Título del video en YouTube. Máximo 100 caracteres (límite de YouTube). */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /** Descripción del video. Máximo 5000 caracteres (límite de YouTube). */
    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** Tags separados por coma. Máximo 500 caracteres totales (límite de YouTube). */
    @Column(name = "tags", columnDefinition = "text")
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(name = "privacy_status", nullable = false, length = 20)
    private YouTubePrivacyStatus privacyStatus;

    /** ID del video en YouTube, llenado cuando {@code status = COMPLETED}. */
    @Column(name = "youtube_video_id", length = 32)
    private String youtubeVideoId;

    /** URL pública del video en YouTube. Forma: https://youtube.com/watch?v=... */
    @Column(name = "youtube_video_url", columnDefinition = "text")
    private String youtubeVideoUrl;

    @Column(name = "bytes_uploaded", nullable = false)
    private Long bytesUploaded;

    /** Tamaño total del archivo en GCS. Null hasta que el worker lo conoce. */
    @Column(name = "bytes_total")
    private Long bytesTotal;

    /** Código semántico del error si {@code status = FAILED}. */
    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /** Cuándo el worker empezó a procesar este job. */
    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    /** Cuándo el job terminó (éxito o fallo definitivo). */
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** Hook JPA: cualquier UPDATE refresca el campo {@code updatedAt}. */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}