package com.example.backend_socialmedia.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Propiedades de configuración para Google Generative AI
 * Se validan automáticamente al iniciar la aplicación
 */
@Configuration
@ConfigurationProperties(prefix = "google")
@Validated
public class GoogleAiProperties {

    @NotBlank(message = "google.generative-ai-api-key no puede estar vacío. Configura GOOGLE_GENERATIVE_AI_API_KEY")
    private String generativeAiApiKey;

    private String tokenVerifyUrl;

    @Positive(message = "google.video-api-timeout-seconds debe ser mayor a 0")
    private int videoApiTimeoutSeconds = 300;

    // Getters y Setters
    public String getGenerativeAiApiKey() { return generativeAiApiKey; }
    public void setGenerativeAiApiKey(String generativeAiApiKey) {
        this.generativeAiApiKey = generativeAiApiKey;
    }

    public String getTokenVerifyUrl() { return tokenVerifyUrl; }
    public void setTokenVerifyUrl(String tokenVerifyUrl) {
        this.tokenVerifyUrl = tokenVerifyUrl;
    }

    public int getVideoApiTimeoutSeconds() { return videoApiTimeoutSeconds; }
    public void setVideoApiTimeoutSeconds(int videoApiTimeoutSeconds) {
        this.videoApiTimeoutSeconds = videoApiTimeoutSeconds;
    }
}

