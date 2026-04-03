package com.example.backend_socialmedia.video.domain;

/**
 * DTO para la solicitud de generación de video
 */
public class GenerateVideoRequest {
    private String title;
    private String description;
    private String prompt;

    public GenerateVideoRequest() {}

    public GenerateVideoRequest(String title, String description, String prompt) {
        this.title = title;
        this.description = description;
        this.prompt = prompt;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}

