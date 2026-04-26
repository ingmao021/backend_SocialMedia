package com.example.backend_socialmedia.video.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GenerateVideoRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200, message = "El título no puede superar 200 caracteres")
    private String title;

    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    private String description;

    @NotBlank(message = "El prompt es obligatorio")
    @Size(min = 5, max = 1000, message = "El prompt debe tener entre 5 y 1000 caracteres")
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
