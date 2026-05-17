package com.socialvideo.youtube.entity;

import com.socialvideo.user.entity.User;
import com.socialvideo.video.entity.Video;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
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
    private UUID id;

    /** FK a {@code users}. Propietario del job. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** FK a {@code videos}. El video a subir. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private YouTubeExportStatus status;

    /** Título del video en YouTube. Máximo 100 caracteres (límite de YouTube). */
    @Column(nullable = false, length = 100)
    private String title;

    /** Descripción del video. Máximo 5000 caracteres (límite de YouTube). */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Tags separados por coma. Máximo 500 caracteres totales (límite de YouTube). */
    @Column(columnDefinition = "TEXT")
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(name = "privacy_status", nullable = false, length = 20)
    private YouTubePrivacyStatus privacyStatus;

    /** ID del video en YouTube. Se llena cuando {@code status = COMPLETED}. */
    @Column(name = "youtube_video_id", length = 32)
    private String youtubeVideoId;

    /** URL pública del video. Forma: https://youtube.com/watch?v=... */
    @Column(name = "youtube_video_url", columnDefinition = "TEXT")
    private String youtubeVideoUrl;

    @Column(name = "bytes_uploaded", nullable = false)
    @Builder.Default
    private Long bytesUploaded = 0L;

    /** Tamaño total del archivo en GCS. Null hasta que el worker lo conoce. */
    @Column(name = "bytes_total")
    private Long bytesTotal;

    /** Código semántico del error si {@code status = FAILED}. */
    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Cuándo el worker empezó a procesar este job. */
    @Column(name = "started_at")
    private Instant startedAt;

    /** Cuándo el job terminó (éxito o fallo definitivo). */
    @Column(name = "completed_at")
    private Instant completedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}