package com.example.backend_socialmedia.video.infrastructure.google;

/**
 * Respuesta de Google para la generación de videos
 */
public class GoogleVideoGenerationResponse {
    private String jobId;
    private String status;
    private String videoUrl;
    private String errorMessage;

    public GoogleVideoGenerationResponse() {}

    public GoogleVideoGenerationResponse(String jobId, String status, String videoUrl, String errorMessage) {
        this.jobId = jobId;
        this.status = status;
        this.videoUrl = videoUrl;
        this.errorMessage = errorMessage;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

