package com.example.backend_socialmedia.youtube.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO que representa la solicitud para publicar un video en YouTube
 */
public class YouTubePublishRequest {

    @NotNull(message = "El ID del video generado es requerido")
    private Long videoId;

    @NotBlank(message = "El título es requerido")
    private String title;

    private String description;

    @NotNull(message = "La visibilidad es requerida")
    private YouTubeVisibility visibility;

    // Constructores
    public YouTubePublishRequest() {
    }

    public YouTubePublishRequest(Long videoId, String title, String description, YouTubeVisibility visibility) {
        this.videoId = videoId;
        this.title = title;
        this.description = description;
        this.visibility = visibility;
    }

    // Getters y Setters
    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
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
}

