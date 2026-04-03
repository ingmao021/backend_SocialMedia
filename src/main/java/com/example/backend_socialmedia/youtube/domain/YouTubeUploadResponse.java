package com.example.backend_socialmedia.youtube.domain;

import java.time.LocalDateTime;

/**
 * DTO que representa la respuesta de una publicación en YouTube
 */
public class YouTubeUploadResponse {

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
    public YouTubeUploadResponse(YouTubeUpload youtubeUpload) {
        this.id = youtubeUpload.getId();
        this.videoId = youtubeUpload.getVideoId();
        this.userId = youtubeUpload.getUserId();
        this.status = youtubeUpload.getStatus();
        this.youtubeVideoId = youtubeUpload.getYoutubeVideoId();
        this.youtubeUrl = youtubeUpload.getYoutubeUrl();
        this.title = youtubeUpload.getTitle();
        this.description = youtubeUpload.getDescription();
        this.visibility = youtubeUpload.getVisibility();
        this.publishedAt = youtubeUpload.getPublishedAt();
        this.errorMessage = youtubeUpload.getErrorMessage();
        this.createdAt = youtubeUpload.getCreatedAt();
        this.updatedAt = youtubeUpload.getUpdatedAt();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getVideoId() {
        return videoId;
    }

    public Long getUserId() {
        return userId;
    }

    public YouTubeUploadStatus getStatus() {
        return status;
    }

    public String getYoutubeVideoId() {
        return youtubeVideoId;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public YouTubeVisibility getVisibility() {
        return visibility;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

