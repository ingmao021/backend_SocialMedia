package com.example.backend_socialmedia.youtube.infrastructure.persistence;

import com.example.backend_socialmedia.youtube.domain.YouTubeUploadStatus;
import com.example.backend_socialmedia.youtube.domain.YouTubeVisibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa una publicación de video en YouTube en la base de datos
 */
@Entity
@Table(name = "youtube_uploads")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YouTubeUploadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private YouTubeUploadStatus status;

    @Column(name = "youtube_video_id", length = 255)
    private String youtubeVideoId;

    @Column(name = "youtube_url", length = 2048)
    private String youtubeUrl;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "visibility", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private YouTubeVisibility visibility;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

