package com.example.backend_socialmedia.youtube.domain;

import java.time.LocalDateTime;

/**
 * Entidad de dominio que representa una publicación de video en YouTube
 */
public class YouTubeUpload {

    private Long id;
    private Long videoId;
    private Long userId;
    private YouTubeUploadStatus status;
    private String youtubeVideoId;
    private String youtubeUrl;
    private String title;
    private String description;
    private YouTubeVisibility visibility;
    private LocalDateTime publishedAt;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor
    public YouTubeUpload(Long videoId, Long userId, String title, String description, YouTubeVisibility visibility) {
        this.videoId = videoId;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.visibility = visibility;
        this.status = YouTubeUploadStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor vacío para JPA
    public YouTubeUpload() {
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public YouTubeUploadStatus getStatus() {
        return status;
    }

    public void setStatus(YouTubeUploadStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String getYoutubeVideoId() {
        return youtubeVideoId;
    }

    public void setYoutubeVideoId(String youtubeVideoId) {
        this.youtubeVideoId = youtubeVideoId;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public void setYoutubeUrl(String youtubeUrl) {
        this.youtubeUrl = youtubeUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public YouTubeVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(YouTubeVisibility visibility) {
        this.visibility = visibility;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        if (errorMessage != null) {
            this.status = YouTubeUploadStatus.FAILED;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

