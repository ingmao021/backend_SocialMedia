package com.example.backend_socialmedia.youtube.infrastructure.google;

/**
 * DTO que representa la respuesta de operaciones en YouTube Data API
 */
public class YouTubePublishResponse {

    private String youtubeVideoId;
    private String youtubeUrl;
    private String status;
    private String errorMessage;

    // Constructor
    public YouTubePublishResponse(String youtubeVideoId, String youtubeUrl, String status, String errorMessage) {
        this.youtubeVideoId = youtubeVideoId;
        this.youtubeUrl = youtubeUrl;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    // Getters y Setters
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

