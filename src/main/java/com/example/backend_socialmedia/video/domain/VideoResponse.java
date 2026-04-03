package com.example.backend_socialmedia.video.domain;

import java.time.LocalDateTime;

/**
 * DTO para la respuesta de video
 */
public class VideoResponse {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String prompt;
    private String status;
    private String videoUrl;
    private String googleJobId;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructores
    public VideoResponse() {}

    public VideoResponse(Video video) {
        this.id = video.getId();
        this.userId = video.getUserId();
        this.title = video.getTitle();
        this.description = video.getDescription();
        this.prompt = video.getPrompt();
        this.status = video.getStatus() != null ? video.getStatus().toString() : null;
        this.videoUrl = video.getVideoUrl();
        this.googleJobId = video.getGoogleJobId();
        this.errorMessage = video.getErrorMessage();
        this.createdAt = video.getCreatedAt();
        this.updatedAt = video.getUpdatedAt();
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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

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

