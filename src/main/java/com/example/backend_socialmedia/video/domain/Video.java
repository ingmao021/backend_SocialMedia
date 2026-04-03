package com.example.backend_socialmedia.video.domain;

import java.time.LocalDateTime;

/**
 * Entidad de dominio que representa un video generado por IA
 */
public class Video {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String prompt;
    private VideoStatus status;
    private String videoUrl;
    private String googleJobId;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor vacío
    public Video() {}

    // Constructor completo
    public Video(Long id, Long userId, String title, String description, String prompt,
                 VideoStatus status, String videoUrl, String googleJobId, String errorMessage,
                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.prompt = prompt;
        this.status = status;
        this.videoUrl = videoUrl;
        this.googleJobId = googleJobId;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public VideoStatus getStatus() { return status; }
    public void setStatus(VideoStatus status) { this.status = status; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getGoogleJobId() { return googleJobId; }
    public void setGoogleJobId(String googleJobId) { this.googleJobId = googleJobId; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

